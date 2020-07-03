package net.labyfy.gradle.minecraft;

import net.labyfy.gradle.LabyfyGradleException;
import net.labyfy.gradle.environment.DeobfuscationEnvironment;
import net.labyfy.gradle.environment.DeobfuscationException;
import net.labyfy.gradle.environment.DeobfuscationUtilities;
import net.labyfy.gradle.environment.EnvironmentCacheFileProvider;
import net.labyfy.gradle.java.compile.JavaCompileHelper;
import net.labyfy.gradle.java.exec.JavaExecutionHelper;
import net.labyfy.gradle.json.JsonConverter;
import net.labyfy.gradle.json.JsonConverterException;
import net.labyfy.gradle.maven.MavenArtifactDownloader;
import net.labyfy.gradle.maven.MavenResolveException;
import net.labyfy.gradle.maven.SimpleMavenRepository;
import net.labyfy.gradle.maven.pom.MavenArtifact;
import net.labyfy.gradle.maven.pom.MavenDependency;
import net.labyfy.gradle.maven.pom.MavenDependencyScope;
import net.labyfy.gradle.maven.pom.MavenPom;
import net.labyfy.gradle.maven.pom.io.PomReader;
import net.labyfy.gradle.minecraft.data.environment.EnvironmentInput;
import net.labyfy.gradle.minecraft.data.manifest.MinecraftManifestVersion;
import net.labyfy.gradle.minecraft.data.manifest.VersionsManifest;
import net.labyfy.gradle.minecraft.data.version.VersionManifest;
import net.labyfy.gradle.minecraft.data.version.VersionedDownload;
import net.labyfy.gradle.minecraft.data.version.VersionedLibrary;
import net.labyfy.gradle.util.RuleChainResolver;
import net.labyfy.gradle.util.TimeStampedFile;
import net.labyfy.gradle.util.Util;
import org.apache.http.client.HttpClient;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MinecraftRepository extends SimpleMavenRepository {
    private static final Logger LOGGER = Logging.getLogger(MinecraftRepository.class);
    private static final String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String MAPPINGS_URL = "https://dl.labymod.net/mappings/index.json";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");

    private final HttpClient httpClient;

    private final TimeStampedFile versionManifestFile;
    private final TimeStampedFile mappingsDefinitionFile;

    private final Path environmentBasePath;
    private final Path versionsDir;
    private final VersionsManifest manifest;
    private final Map<String, EnvironmentInput> versionedEnvironments;

    /**
     * Instantiates the minecraft repository accessor.
     *
     * @param repoBase   The base directory of the repository
     * @param cacheDir   The directory to keep temporary files in
     * @param httpClient The HTTP client to use
     * @throws IOException If an I/O error occurs while creating the directory
     */
    public MinecraftRepository(Path repoBase, Path cacheDir, HttpClient httpClient) throws IOException {
        super(repoBase);
        this.httpClient = httpClient;

        this.versionManifestFile = new TimeStampedFile(cacheDir.resolve("version-manifest.json"));
        this.mappingsDefinitionFile = new TimeStampedFile(cacheDir.resolve("mappings.json"));

        this.environmentBasePath = cacheDir.resolve("environments");
        this.versionsDir = cacheDir.resolve("versions");

        if (!Files.isDirectory(versionsDir)) {
            Files.createDirectories(versionsDir);
        }

        if (httpClient != null) {
            this.versionManifestFile.update(httpClient, VERSION_MANIFEST_URL, DATE_TIME_FORMATTER);
            this.mappingsDefinitionFile.update(httpClient, MAPPINGS_URL, DATE_TIME_FORMATTER);
        } else {
            if (!Files.isRegularFile(versionManifestFile.toPath())) {
                throw new LabyfyGradleException("Versions manifest does not exist, but cant be downloaded due to " +
                        "gradle operating in offline mode");
            } else if (!Files.isRegularFile(mappingsDefinitionFile.toPath())) {
                throw new LabyfyGradleException("Mappings definition file does not exist, but cant be downloaded due " +
                        "to gradle operating in offline mode");
            }
        }

        this.manifest = readVersionsManifest();
        this.versionedEnvironments = readMappings();
    }

    private VersionsManifest readVersionsManifest() throws IOException {
        try (InputStream stream = Files.newInputStream(this.versionManifestFile.toPath())) {
            // Try to convert the file into the given Java object using Jackson
            return JsonConverter.streamToObject(stream, VersionsManifest.class);
        } catch (JsonConverterException e) {
            throw new IOException("Failed to convert version manifest from json", e);
        }
    }

    private Map<String, EnvironmentInput> readMappings() throws IOException {
        try (InputStream stream = Files.newInputStream(this.mappingsDefinitionFile.toPath())) {
            return JsonConverter.streamToObject(
                    stream,
                    JsonConverter.OBJECT_MAPPER
                            .getTypeFactory()
                            .constructMapType(HashMap.class, String.class, EnvironmentInput.class)
            );
        } catch (JsonConverterException e) {
            throw new IOException("Failed to convert mappings from json", e);
        }
    }

    /**
     * Determines if the repository has default mappings available for the given version.
     *
     * @param version The version to check if mappings are available for
     * @return {@code true} if mappings default for the version are present, {@code false} otherwise
     */
    public boolean hasDefaultEnvironmentFor(String version) {
        return versionedEnvironments.containsKey(version);
    }

    /**
     * Retrieves the default environment for the given version.
     *
     * @param version The version to retrieve the environment for
     * @return The default deobfuscation environment
     * @throws IllegalArgumentException If there is no default environment available for the given version
     */
    public DeobfuscationEnvironment defaultEnvironment(String version) {
        if(!versionedEnvironments.containsKey(version)) {
            throw new IllegalArgumentException("No default environment available for version " + version);
        }

        return DeobfuscationEnvironment.createFor(versionedEnvironments.get(version));
    }

    /**
     * Installs the given minecraft version using the given deobfuscation environments or by automatically detecting
     * them if not given
     *
     * @param version            The version to install
     * @param environment        The environment to use for deobfuscation
     * @param internalRepository The repository to use for storing artifacts required while installing
     * @param downloader         The downloader to use for installing internal artifacts
     * @param project            The project to use for utility function basics
     * @throws IllegalArgumentException If no default deobfuscation environments are available for the given version and
     *                                  no environments are given
     * @throws IllegalArgumentException If the given minecraft version does not exist
     * @throws IOException              If an I/O error occurs
     */
    public void install(
            String version,
            DeobfuscationEnvironment environment,
            SimpleMavenRepository internalRepository,
            MavenArtifactDownloader downloader,
            Project project
    ) throws IOException {
        MinecraftManifestVersion manifestVersion = null;

        for (MinecraftManifestVersion availableVersion : manifest.getVersions()) {
            // Try to find the given version by comparing the ID's
            if (availableVersion.getId().equals(version)) {
                manifestVersion = availableVersion;
            }
        }

        if (manifestVersion == null) {
            // Bail out if the given version does not exist
            throw new IllegalArgumentException("No such minecraft version " + version);
        }

        TimeStampedFile clientVersionJson = new TimeStampedFile(versionsDir.resolve(version + ".json"));

        if (httpClient != null) {
            clientVersionJson.update(httpClient, manifestVersion.getUrl().toExternalForm(), DATE_TIME_FORMATTER);
        } else if (!Files.isRegularFile(clientVersionJson.toPath())) {
            // The version json does not exist and we are operating in offline mode
            throw new LabyfyGradleException("Version manifest for " + version + " not present and unable to download " +
                    "due to gradle working in offline mode");
        }

        VersionManifest versionManifest = getVersionManifest(version);

        // Install client and server jar into the repository if available
        MavenPom clientJar = installVariantIfExist(
                versionManifest, "client", true, internalRepository, downloader);
        MavenPom serverJar = installVariantIfExist(
                versionManifest, "server", false, null, null);

        if (clientJar == null && serverJar == null) {
            // We can't continue if the version contains none of the known artifacts
            throw new LabyfyGradleException("Could not download client nor server jar");
        }

        try {
            environment.runDeobfuscation(clientJar, serverJar, new DeobfuscationUtilities(
                    downloader,
                    this,
                    internalRepository,
                    httpClient,
                    new EnvironmentCacheFileProvider(environmentBasePath.resolve(environment.name())),
                    new JavaExecutionHelper(project),
                    new JavaCompileHelper(project)
            ));
        } catch (DeobfuscationException e) {
            throw new LabyfyGradleException("Failed to deobfuscate " + version, e);
        }
    }

    private MavenPom installVariantIfExist(
            VersionManifest manifest,
            String variant,
            boolean includeDependencies,
            SimpleMavenRepository internalRepository,
            MavenArtifactDownloader downloader
    ) throws IOException {
        VersionedDownload download = manifest.getDownloads().get(variant);
        if (download == null) {
            // The requested variant does not exist
            return null;
        }

        Path targetPath = getArtifactPath("net.minecraft", variant, manifest.getId());
        if (!Files.exists(targetPath)) {
            LOGGER.lifecycle("Downloading minecraft {} {}", variant, manifest.getId());
            Util.download(httpClient, download.getUrl().toExternalForm(), targetPath);
        }

        MavenPom pom = createPom(manifest, variant, includeDependencies);

        // If dependencies should be included install them into the internal repository
        if (includeDependencies) {
            try {
                downloader.installAll(pom, internalRepository, false);
            } catch (MavenResolveException e) {
                // This will hopefully never happen
                throw new LabyfyGradleException("Minecraft " + variant + " has broken dependencies", e);
            }
        }

        return pom;
    }

    private MavenPom createPom(VersionManifest manifest, String variant, boolean includeDependencies) throws IOException {
        Path targetPath = getArtifactPath("net.minecraft", variant, manifest.getId(), null, "pom");
        if (Files.exists(targetPath)) {
            // If the POM exists already we don't need to rewrite it
            return PomReader.read(targetPath);
        }

        // Keep a list of already added artifacts, the minecraft manifest has duplicated dependencies
        Set<MavenArtifact> mavenArtifacts = new HashSet<>();
        MavenPom pom = new MavenPom("net.minecraft", variant, manifest.getId());

        if (includeDependencies) {
            for (VersionedLibrary library : manifest.getLibraries()) {
                if (!RuleChainResolver.testRuleChain(library.getRules())) {
                    // If the library does not match the rule chain ignore it,
                    // the generated POM's will only be used locally and thus only need to
                    // match the current environment
                    continue;
                }

                MavenArtifact artifact = library.getName();
                if (!mavenArtifacts.contains(artifact)) {
                    // Found a new dependency, add it to the POM
                    pom.addDependency(new MavenDependency(artifact, MavenDependencyScope.COMPILE));
                    mavenArtifacts.add(artifact);
                }

                String nativeClassifier = RuleChainResolver.resolveNativeClassifier(library);
                if (nativeClassifier != null) {
                    // The dependency has a native variant, construct a new artifact with the native classifier
                    MavenArtifact nativeArtifact = new MavenArtifact(artifact);
                    nativeArtifact.setClassifier(nativeClassifier);

                    if (!mavenArtifacts.contains(nativeArtifact)) {
                        // If the native dependency is not added already, add it to the POM
                        pom.addDependency(new MavenDependency(nativeArtifact, MavenDependencyScope.RUNTIME));
                        mavenArtifacts.add(nativeArtifact);
                    }
                }
            }
        }

        // Write the generated POM
        addPom(pom);
        return pom;
    }

    /**
     * Retrieves the version manifest for the given version.
     *
     * @param version The version to retrieve the manifest for
     * @return The manifest of the given version
     * @throws IOException If an I/O error occurs while reading the version file
     * @throws IllegalArgumentException If the given version is not installed
     */
    public VersionManifest getVersionManifest(String version) throws IOException {
        Path versionsFile = versionsDir.resolve(version + ".json");
        if(!Files.exists(versionsFile)) {
            // We don't have the version installed at all
            throw new IllegalArgumentException("Minecraft version " + version + " is not installed");
        }

        try (InputStream stream = Files.newInputStream(versionsFile)) {
            return JsonConverter.streamToObject(stream, VersionManifest.class);
        } catch (JsonConverterException e) {
            // Probably a corrupted file
            throw new IOException("Failed to convert version manifest from json", e);
        }
    }
}
