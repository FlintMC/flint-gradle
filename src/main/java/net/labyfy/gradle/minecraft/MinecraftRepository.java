package net.labyfy.gradle.minecraft;

import net.labyfy.gradle.LabyfyGradleException;
import net.labyfy.gradle.environment.DeobfuscationEnvironment;
import net.labyfy.gradle.json.JsonConverter;
import net.labyfy.gradle.json.JsonConverterException;
import net.labyfy.gradle.minecraft.data.environment.EnvironmentInput;
import net.labyfy.gradle.minecraft.data.manifest.MinecraftManifestVersion;
import net.labyfy.gradle.minecraft.data.manifest.VersionsManifest;
import net.labyfy.gradle.minecraft.data.version.*;
import net.labyfy.gradle.util.RuleChainResolver;
import net.labyfy.gradle.util.TimeStampedFile;
import net.labyfy.gradle.util.Util;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.logging.LoggingManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class MinecraftRepository {
    private static final Logger LOGGER = Logging.getLogger(MinecraftRepository.class);
    private static final String VERSION_MANIFEST_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    private static final String MAPPINGS_URL = "https://dl.labymod.net/mappings/index.json";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");

    private boolean offlineMode;

    private final CloseableHttpClient httpClient;

    private final TimeStampedFile versionManifestFile;
    private final TimeStampedFile mappingsDefinitionFile;

    private final Path versionsDir;
    private final Path repoDir;
    private final VersionsManifest manifest;
    private final Map<String, EnvironmentInput> versionedEnvironments;

    /**
     * Instantiates the minecraft repository accessor.
     *
     * @param gradle The current gradle instance
     * @throws IOException If an I/O error occurs while creating the directory
     */
    public MinecraftRepository(Gradle gradle) throws IOException {
        this.offlineMode = gradle.getStartParameter().isOffline();
        this.httpClient = offlineMode ? null :
                HttpClientBuilder.create().useSystemProperties().setUserAgent("Labyfy-Gradle v0.3.0").build();

        Path labyfyGradlePath = gradle.getGradleUserHomeDir().toPath().resolve("caches/labyfy-gradle");
        this.versionManifestFile = new TimeStampedFile(labyfyGradlePath.resolve("version-manifest.json"));
        this.mappingsDefinitionFile = new TimeStampedFile(labyfyGradlePath.resolve("mappings.json"));
        this.versionsDir = labyfyGradlePath.resolve("versions");
        this.repoDir = labyfyGradlePath.resolve("minecraft-repository");

        if (!Files.isDirectory(versionsDir)) {
            Files.createDirectories(versionsDir);
        }

        if (!Files.isDirectory(repoDir)) {
            Files.createDirectories(repoDir);
        }

        if (!offlineMode) {
            assert httpClient != null;
            this.versionManifestFile.update(httpClient, VERSION_MANIFEST_URL, DATE_TIME_FORMATTER);
            // this.mappingsDefinitionFile.update(httpClient, MAPPINGS_URL, DATE_TIME_FORMATTER);
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
     * Installs the given minecraft version by automatically detecting the available
     * deobfuscation environments.
     *
     * @param version The version to install
     * @throws IllegalArgumentException If no default deobfuscation environments are available for the given version
     * @throws IllegalArgumentException If the given minecraft version does not exist
     * @throws IOException              If an I/O error occurs
     */
    public void install(String version) throws IOException {
        install(version, new ArrayList<>());
    }

    /**
     * Installs the given minecraft version using the given deobfuscation environments or by automatically detecting
     * them if not given
     *
     * @param version      The version to install
     * @param environments The environments to use, or {@code null} for auto detection
     * @throws IllegalArgumentException If no default deobfuscation environments are available for the given version and
     *                                  no environments are given
     * @throws IllegalArgumentException If the given minecraft version does not exist
     * @throws IOException              If an I/O error occurs
     */
    public void install(String version, List<DeobfuscationEnvironment> environments) throws IOException {
        MinecraftManifestVersion manifestVersion = null;

        for (MinecraftManifestVersion availableVersion : manifest.getVersions()) {
            // Try to find the given version by comparing the ID's
            if (availableVersion.getId().equals(version)) {
                manifestVersion = availableVersion;
            }
        }

        if (manifestVersion == null) {
            // Bail out if the given version does not exist
            throw new IllegalArgumentException("No such minecraft " + version);
        }

        if (environments.isEmpty() && !hasDefaultEnvironmentFor(version)) {
            throw new IllegalArgumentException("No default deobfuscation environments available for " + version);
        } else if (environments.isEmpty()) {
            // Construct default deobfuscation environments
            environments = DeobfuscationEnvironment.createFor(versionedEnvironments.get(version));
        }

        TimeStampedFile clientVersionJson = new TimeStampedFile(versionsDir.resolve(version + ".json"));

        if (!offlineMode) {
            clientVersionJson.update(httpClient, manifestVersion.getUrl().toExternalForm(), DATE_TIME_FORMATTER);
        } else if (!Files.isRegularFile(clientVersionJson.toPath())) {
            // The version json does not exist and we are operating in offline mode
            throw new LabyfyGradleException("Version manifest for " + version + " not present and unable to download " +
                    "due to gradle working in offline mode");
        }

        VersionManifest versionManifest;

        try (InputStream stream = Files.newInputStream(clientVersionJson.toPath())) {
            versionManifest = JsonConverter.streamToObject(stream, VersionManifest.class);
        } catch (JsonConverterException e) {
            throw new IOException("Failed to convert version manifest from json", e);
        }

        // Install client and server jar into the repository if available
        Path clientJar = installVariantIfExist(versionManifest, "client");
        Path serverJar = installVariantIfExist(versionManifest, "server");
    }

    private Path installVariantIfExist(VersionManifest manifest, String variant) throws IOException {
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

        createPom(manifest, variant);

        return targetPath;
    }

    private void createPom(VersionManifest manifest, String variant) throws IOException {
        Path targetPath = getArtifactPath("net.minecraft", variant, manifest.getId(), null, "pom");
        if (Files.exists(targetPath)) {
            return;
        }

        Set<VersionedLibraryName> libraryNames = new HashSet<>();

        Document document;
        try {
            // Configure the main XML tag with standalone=true and version=1.0
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            document.setXmlStandalone(true);
            document.setXmlVersion("1.0");

            // Set create the root tag and add the schemas
            Element root = document.createElement("project");
            root.setAttribute("xsi:schemaLocation",
                    "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd");
            root.setAttribute("xmlns", "http://maven.apache.org/POM/4.0.0");
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            document.appendChild(root);

            // Create the model version tag with the value 4.0.0
            Element modelVersionElement = document.createElement("modelVersion");
            modelVersionElement.appendChild(document.createTextNode("4.0.0"));
            root.appendChild(modelVersionElement);

            // Create the groupId tag
            Element groupIdElement = document.createElement("groupId");
            groupIdElement.appendChild(document.createTextNode("net.minecraft"));
            root.appendChild(groupIdElement);

            // Create the artifactId tag
            Element artifactIdElement = document.createElement("artifactId");
            artifactIdElement.appendChild(document.createTextNode(variant));
            root.appendChild(artifactIdElement);

            // Create the version tag
            Element versionElement = document.createElement("version");
            versionElement.appendChild(document.createTextNode(manifest.getId()));
            root.appendChild(versionElement);

            // Create the dependencies tag
            Element dependenciesElement = document.createElement("dependencies");
            for (VersionedLibrary library : manifest.getLibraries()) {
                // First of all check if the library even applies to the current environment
                if (!RuleChainResolver.testRuleChain(library.getRules())) {
                    // Library does not apply
                    continue;
                }

                // Extract the artifact id
                String group = library.getName().getGroup();
                String name = library.getName().getName();
                String version = library.getName().getVersion();
                String classifier = library.getName().getClassifier(); // Probably always null

                // Check for duplicates
                if(!libraryNames.contains(library.getName())) {
                    // Create the main dependency
                    dependenciesElement.appendChild(createDependencyElement(
                            document,
                            group,
                            name,
                            version,
                            classifier,
                            "compile"
                    ));
                }

                libraryNames.add(library.getName());

                // Check if there is also a native classifier dependency
                String nativeClassifier = RuleChainResolver.resolveNativeClassifier(library);
                if (nativeClassifier != null) {
                    VersionedLibraryName nativeName =
                            new VersionedLibraryName(group, name, version, nativeClassifier, null);

                    if(!libraryNames.contains(nativeName)) {
                        // If so, add it as runtime, if it has not been added already
                        dependenciesElement.appendChild(createDependencyElement(
                                document,
                                group,
                                name,
                                version,
                                nativeClassifier,
                                "runtime"
                        ));

                        libraryNames.add(nativeName);
                    }
                }
            }

            // Add the dependencies element to the root
            root.appendChild(dependenciesElement);
        } catch (ParserConfigurationException e) {
            throw new IOException("Failed to create document", e);
        }

        try(OutputStream stream = Files.newOutputStream(targetPath)) {
            // Convert our document into a writeable source and prepare a result
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(stream);

            // Run a transformer over it to convert it to XML
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);
        } catch (TransformerConfigurationException e) {
            throw new IOException("Failed to create transformer", e);
        } catch (TransformerException e) {
            throw new IOException("Failed to transform document", e);
        }
    }

    /**
     * Creates a maven pom XML dependency element.
     *
     * @param document   The document to create the element on
     * @param group      The groupId of the dependency
     * @param name       The artifactId of the dependency
     * @param version    The version of the dependency
     * @param classifier The classifier of the dependency, or null if none
     * @param scope      The scope of the dependency
     * @return The created element
     */
    private Element createDependencyElement(
            Document document, String group, String name, String version, String classifier, String scope) {
        Element dependencyElement = document.createElement("dependency");

        // Create the groupId tag
        Element groupIdElement = document.createElement("groupId");
        groupIdElement.appendChild(document.createTextNode(group));
        dependencyElement.appendChild(groupIdElement);

        // Create the artifactId tag
        Element artifactIdElement = document.createElement("artifactId");
        artifactIdElement.appendChild(document.createTextNode(name));
        dependencyElement.appendChild(artifactIdElement);

        // Create the version tag
        Element versionElement = document.createElement("version");
        versionElement.appendChild(document.createTextNode(version));
        dependencyElement.appendChild(versionElement);

        if (classifier != null) {
            // Create the classifier element
            Element classifierElement = document.createElement("classifier");
            classifierElement.appendChild(document.createTextNode(classifier));
            dependencyElement.appendChild(classifierElement);
        }

        // Create the scope element
        Element scopeElement = document.createElement("scope");
        scopeElement.appendChild(document.createTextNode(scope));
        dependencyElement.appendChild(scopeElement);

        return dependencyElement;
    }

    /**
     * Retrieves the directory for an artifact.
     *
     * @param group   The group of the artifact
     * @param name    The name of the artifact
     * @param version The version of the artifact
     * @return The path to the directory of the artifact
     */
    public Path getArtifactDirPath(String group, String name, String version) {
        return this.repoDir
                .resolve(group.replace('.', '/'))
                .resolve(name)
                .resolve(version);
    }

    /**
     * Retrieves the path for an artifact.
     *
     * @param group   The group of the artifact
     * @param name    The name of the artifact
     * @param version The version of the artifact
     * @return The path to the artifact
     */
    public Path getArtifactPath(String group, String name, String version) {
        return getArtifactDirPath(group, name, version).resolve(name + '-' + version + ".jar");
    }

    /**
     * Retrieves the path for an artifact.
     *
     * @param group      The group of the artifact
     * @param name       The name of the artifact
     * @param version    The version of the artifact
     * @param classifier The classifier of the artifact
     * @return The path to the artifact
     */
    public Path getArtifactPath(String group, String name, String version, String classifier) {
        return getArtifactDirPath(group, name, version).resolve(name + '-' + version +
                (classifier == null ? "" : "-" + classifier) + ".jar");
    }

    /**
     * Retrieves the path for an artifact.
     *
     * @param group      The group of the artifact
     * @param name       The name of the artifact
     * @param version    The version of the artifact
     * @param classifier The classifier of the artifact
     * @param extension  The extension of the artifact (without the '.')
     * @return The path to the artifact
     */
    public Path getArtifactPath(String group, String name, String version, String classifier, String extension) {
        return getArtifactDirPath(group, name, version).resolve(name + '-' + version +
                (classifier == null ? "" : "-" + classifier) + "." + (extension == null ? "jar" : extension));
    }
}
