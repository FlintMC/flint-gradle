package net.labyfy.gradle.environment.mcp;

import net.labyfy.gradle.environment.DeobfuscationEnvironment;
import net.labyfy.gradle.environment.DeobfuscationException;
import net.labyfy.gradle.environment.DeobfuscationUtilities;
import net.labyfy.gradle.environment.EnvironmentCacheFileProvider;
import net.labyfy.gradle.java.exec.JavaExecutionResult;
import net.labyfy.gradle.maven.MavenArtifactDownloader;
import net.labyfy.gradle.maven.MavenResolveException;
import net.labyfy.gradle.maven.SimpleMavenRepository;
import net.labyfy.gradle.maven.pom.MavenArtifact;
import net.labyfy.gradle.maven.pom.MavenPom;
import net.labyfy.gradle.minecraft.MinecraftRepository;
import net.labyfy.gradle.minecraft.data.environment.ModCoderPackInput;
import net.labyfy.gradle.util.Util;
import org.apache.http.client.HttpClient;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Implementation of the MCP as a deobfuscation environment.
 */
public class ModCoderPackEnvironment implements DeobfuscationEnvironment {
    private static final Logger LOGGER = Logging.getLogger(ModCoderPackEnvironment.class);

    private final ModCoderPackInput input;

    /**
     * Constructs a new MCP deobfuscation environment with the given input data.
     *
     * @param input The data to obtain MCP files from
     */
    public ModCoderPackEnvironment(ModCoderPackInput input) {
        this.input = input;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String name() {
        return "ModCoderPack";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void runDeobfuscation(MavenPom clientPom, MavenPom serverPom, DeobfuscationUtilities utilities)
            throws DeobfuscationException {
        // Get a few utilities classes out of the instance variable
        EnvironmentCacheFileProvider cacheFileProvider = utilities.getCacheFileProvider();
        HttpClient httpClient = utilities.getHttpClient();

        // Download and extract the inputs if required
        Path mcpConfigOutput = downloadAndExtractZip(
                cacheFileProvider,
                httpClient,
                input.getConfigDownload().toExternalForm(),
                "mcp-config_" + input.getConfigVersion());
        Path mappingsOutput = downloadAndExtractZip(
                cacheFileProvider,
                httpClient,
                input.getMappingsDownload().toExternalForm(),
                "mcp-mappings_" + input.getMappingsVersion());

        // Create the runner
        ModCoderPackRun run = new ModCoderPackRun(
                clientPom,
                serverPom,
                utilities,
                mcpConfigOutput,
                mappingsOutput
        );

        try {
            run.loadData();
        } catch (IOException e) {
            throw new DeobfuscationException("Failed to load data for MCP run", e);
        }

        // Save all sides which should be executed
        List<String> sides = new ArrayList<>();

        if (clientPom != null) {
            // If the client exists, deobfuscate it
            sides.add("client");
        }

        if (serverPom != null) {
            // If the server exists, deobfuscate it
            sides.add("server");
        }

        if (clientPom != null && serverPom != null) {
            // If client and server exist, create the joined/merged jars too
            sides.add("joined");
        }

        // Try to retrieve the version
        String version = null;
        if (clientPom != null) {
            version = clientPom.getVersion();
        }

        // If the client has not been set, try to retrieve the version
        // from the server
        if (version == null && serverPom != null) {
            version = serverPom.getVersion();
        } else if (serverPom != null && !version.equals(serverPom.getVersion())) {
            throw new DeobfuscationException("Client/server version mismtach, client: " + clientPom.getVersion() +
                    ", server: " + serverPom.getVersion());
        }

        for (String side : sides) {
            // Prepare all sides
            run.prepare(side);
        }

        Map<String, Path> outputs = new HashMap<>();

        for (String side : sides) {
            // Execute all sides
            outputs.put(side, run.execute(side));
        }

        // Construct the CSV remapper
        CsvRemapper remapper = new CsvRemapper();
        try {
            Files.walk(mappingsOutput)
                    .filter(Files::isRegularFile)
                    .filter((path) -> path.getFileName().toString().endsWith(".csv"))
                    .forEach((path) -> {
                        try {
                            // Load all CSV files as mappings
                            remapper.loadCsv(path);
                        } catch (IOException e) {
                            // Will be caught by the block later down, we need to rethrow as unchecked
                            // as the lambda can't throw
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (UncheckedIOException | IOException e) {
            throw new DeobfuscationException("Failed to initialize remapper", e);
        }

        // Retrieve utility classes
        MinecraftRepository minecraftRepository = utilities.getMinecraftRepository();

        for (Map.Entry<String, Path> output : outputs.entrySet()) {
            // Extract every side and the associated jar
            String side = output.getKey();
            Path srgArtifactPath = output.getValue();

            // Generate output artifact
            MavenArtifact outputArtifact = new MavenArtifact(
                    "net.minecraft",
                    side,
                    version,
                    "mcp-sources"
            );

            // Retrieve the path the artifact should be written to
            Path targetArtifactPath = minecraftRepository.getArtifactPath(outputArtifact);
            if (!Files.isDirectory(targetArtifactPath.getParent())) {
                // Make sure we can write the artifact
                try {
                    Files.createDirectories(targetArtifactPath.getParent());
                } catch (IOException e) {
                    throw new DeobfuscationException("Failed to create parent directory for target artifact", e);
                }
            }

            try {
                LOGGER.lifecycle("Remapping SRG to deobfuscated for {} {}", side, version);
                remapper.remapSourceJar(srgArtifactPath, targetArtifactPath);
            } catch (IOException e) {
                throw new DeobfuscationException("Failed to remap " + side + " " + version, e);
            }
        }
    }

    /**
     * Downloads and extracts a ZIP if the output does not exist already.
     *
     * @param cacheFileProvider The provider to use for file caching
     * @param httpClient        The HTTP client to use for downloading
     * @param url               The URL to retrieve the file from
     * @param outputName        The name of the output file
     * @return The path to the extracted directory
     * @throws DeobfuscationException If the file fails to download or extract
     */
    private Path downloadAndExtractZip(
            EnvironmentCacheFileProvider cacheFileProvider,
            HttpClient httpClient,
            String url,
            String outputName
    ) throws DeobfuscationException {
        // Retrieve the paths to write to
        Path outputZip = cacheFileProvider.file(outputName + ".zip");
        Path outputDir = cacheFileProvider.directory(outputName);

        if (!Files.exists(outputZip) && !Files.isDirectory(outputDir)) {
            // If both paths don't exist, download the zip
            if (httpClient == null) {
                throw new DeobfuscationException("Can't download " + outputName + " when working in offline mode");
            }

            try {
                LOGGER.lifecycle("Downloading {}", outputName);
                Util.download(httpClient, url, outputZip);
            } catch (IOException e) {
                throw new DeobfuscationException("Failed to download " + outputName, e);
            }
        }

        if (!Files.isDirectory(outputDir)) {
            // If the output path does not exist, extract the zip
            try {
                Util.extractZip(outputZip, outputDir);
            } catch (IOException e) {
                throw new DeobfuscationException("Failed to extract zip for " + outputName, e);
            }
        }

        return outputDir;
    }
}
