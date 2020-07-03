package net.labyfy.gradle.environment.mcp;

import net.labyfy.gradle.environment.*;
import net.labyfy.gradle.java.exec.JavaExecutionResult;
import net.labyfy.gradle.maven.SimpleMavenRepository;
import net.labyfy.gradle.maven.pom.MavenArtifact;
import net.labyfy.gradle.maven.pom.MavenDependency;
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
        String version = getVersion(clientPom, serverPom);

        List<Path> clientLibraries = null;
        if (clientPom != null) {
            SimpleMavenRepository internalRepository = utilities.getInternalRepository();

            // Collect the client libraries if the client POM is available
            clientLibraries = new ArrayList<>();

            for (MavenDependency dependency : clientPom.getDependencies()) {
                if (!internalRepository.isInstalled(dependency)) {
                    throw new IllegalStateException("The minecraft dependency " + dependency + " is not installed");
                }

                // Add the path of the artifact to the libraries
                clientLibraries.add(internalRepository.getArtifactPath(dependency));
            }
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

        // Construct the source jar processor
        SourceJarProcessor processor = new SourceJarProcessor();

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

        // Index the actions
        processor.addAction(remapper);
        processor.addAction(new ForgeAdditionStripper());

        // Retrieve utility classes
        MinecraftRepository minecraftRepository = utilities.getMinecraftRepository();

        for (Map.Entry<String, Path> output : outputs.entrySet()) {
            // Extract every side and the associated jar
            String side = output.getKey();
            Path srgArtifactPath = output.getValue();

            // Generate output artifact
            MavenArtifact sourcesArtifact = new MavenArtifact(
                    "net.minecraft",
                    side,
                    version,
                    getClassifier(true)
            );

            // Retrieve the path the artifact should be written to
            Path sourcesTargetArtifactPath = minecraftRepository.getArtifactPath(sourcesArtifact);
            if (!Files.isDirectory(sourcesTargetArtifactPath.getParent())) {
                // Make sure we can write the artifact
                try {
                    Files.createDirectories(sourcesTargetArtifactPath.getParent());
                } catch (IOException e) {
                    throw new DeobfuscationException("Failed to create parent directory for target artifact", e);
                }
            }

            try {
                // Remap the SRG source jar to deobfuscated jar
                LOGGER.lifecycle("Processing SRG to deobfuscated for {} {}", side, version);
                processor.process(srgArtifactPath, sourcesTargetArtifactPath);
            } catch (IOException e) {
                throw new DeobfuscationException("Failed to process " + side + " " + version, e);
            }

            if (clientLibraries != null) {
                // Recompilation can only be done if the client libraries are known
                Path sourceDir = null;

                // Generate output artifact
                MavenArtifact outputArtifact = new MavenArtifact(
                        "net.minecraft",
                        side,
                        version,
                        getClassifier(false)
                );

                try {
                    // Extract the sources jar for recompilation
                    sourceDir = Util.temporaryDir();
                    Util.extractZip(sourcesTargetArtifactPath, sourceDir);

                    LOGGER.lifecycle("Recompiling {} {}", side, version);

                    // Set up the compilation
                    JavaExecutionResult compilationResult = utilities.getJavaCompileHelper().compile(
                            sourceDir,
                            clientLibraries,
                            minecraftRepository.getArtifactPath(outputArtifact)
                    );

                    if (compilationResult.getExitCode() != 0) {
                        // Compilation failed, bail out
                        LOGGER.error("Minecraft {} {} failed to recompile", side, version);
                        LOGGER.error("javac output:");
                        LOGGER.error(compilationResult.getStdout());
                        LOGGER.error("javac error:");
                        LOGGER.error(compilationResult.getStderr());
                        throw new DeobfuscationException("Failed to recompile " + side + " " + version);
                    } else {
                        LOGGER.lifecycle("Done!");
                    }

                    Path pomPath = minecraftRepository.getPomPath(outputArtifact);
                    if (!Files.exists(pomPath)) {
                        // There is no POM for the given artifact, generate one
                        MavenPom pom = new MavenPom(outputArtifact);
                        pom.addDependencies(clientPom.getDependencies());
                        minecraftRepository.addPom(pom);
                    }
                } catch (IOException e) {
                    throw new DeobfuscationException("IO exception while deobfuscating " + side + " " + version, e);
                } finally {
                    if (sourceDir != null) {
                        try {
                            // Try to delete the temporary source directory
                            Util.nukeDirectory(sourceDir, true);
                        } catch (IOException e) {
                            LOGGER.warn("Failed to delete temporary directory {}", sourceDir.toString());
                        }
                    }
                }
            } else {
                LOGGER.warn("Can't recompile {} {}, missing client libraries", side, version);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @param client
     * @param server
     */
    @Override
    public Collection<MavenArtifact> getCompileArtifacts(MavenArtifact client, MavenArtifact server) {
        String version = getVersion(client, server);

        // We only need the client artifact for compilation, this might change at some point
        // if we support servers
        return Collections.singletonList(getJoinedArtifact(version));
    }

    /**
     * {@inheritDoc}
     * @param client
     * @param server
     */
    @Override
    public Collection<MavenArtifact> getRuntimeArtifacts(MavenArtifact client, MavenArtifact server) {
        String version = getVersion(client, server);

        // The runtime always requires the joined artifact
        return Collections.singletonList(getJoinedArtifact(version));
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

    /**
     * Retrieves the version of the client and server POM.
     *
     * @param clientPom The client artifact, may be null
     * @param serverPom The server artifact, may be null
     * @return The version of the client and server POM
     * @throws IllegalArgumentException If the version of the client and server POM mismatch or both client and server
     *                                  POM are null
     */
    private String getVersion(MavenArtifact clientPom, MavenArtifact serverPom) {
        String version = null;
        if(clientPom != null) {
            // The client is given, retrieve its version
            version = clientPom.getVersion();
        }

        if(serverPom != null) {
            // The server is given
            if(version != null && !serverPom.getVersion().equals(version)) {
                // Client and server version are not equal, this is not allowed
                throw new IllegalArgumentException("Client and server version mismatch, client = "
                        + clientPom.getVersion() + ", server = " + serverPom.getVersion());
            } else if(version == null) {
                // If the client has not been set, use the server version
                version = serverPom.getVersion();
            }
        }

        if(version == null) {
            // Received neither a server nor a client POM, this is not allowed
            throw new IllegalArgumentException("Both client and server POM are null");
        }

        return version;
    }

    /**
     * Retrieves the classifier for the current configuration.
     *
     * @param sources If {@code true}, the returned classifier will be a sources classifier
     * @return The generated classifier
     */
    private String getClassifier(boolean sources) {
        return "mcp-" + input.getConfigVersion() + "_" + input.getMappingsVersion() + (sources ? "-sources" : "");
    }

    /**
     * Retrieves the client artifact of the current configuration.
     *
     * @param version The version of minecraft
     * @return The client artifact with the given version
     */
    private MavenArtifact getClientArtifact(String version) {
        return new MavenArtifact("net.minecraft", "client", version, getClassifier(false));
    }

    /**
     * Retrieves the server artifact of the current configuration.
     *
     * @param version The version of minecraft
     * @return The server artifact with the given version
     */
    private MavenArtifact getServerArtifact(String version) {
        return new MavenArtifact("net.minecraft", "server", version, getClassifier(false));
    }

    /**
     * Retrieves the joined artifact of the current configuration.
     *
     * @param version The version of minecraft
     * @return The joined artifact with the given version
     */
    private MavenArtifact getJoinedArtifact(String version) {
        return new MavenArtifact("net.minecraft", "joined", version, getClassifier(false));
    }
}
