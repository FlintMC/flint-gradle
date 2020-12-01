package net.flintmc.gradle.manifest.tasks;

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.json.JsonConverter;
import net.flintmc.gradle.manifest.cache.BoundMavenDependencies;
import net.flintmc.gradle.manifest.cache.StaticFileChecksums;
import net.flintmc.gradle.manifest.data.*;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.property.FlintPluginProperties;
import net.flintmc.installer.impl.repository.models.DependencyDescriptionModel;
import net.flintmc.installer.impl.repository.models.PackageModel;
import net.flintmc.installer.impl.repository.models.install.DownloadFileDataModel;
import net.flintmc.installer.impl.repository.models.install.DownloadMavenDependencyDataModel;
import net.flintmc.installer.impl.repository.models.install.InstallInstructionModel;
import net.flintmc.installer.impl.repository.models.install.InstallInstructionTypes;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.*;

/**
 * Task generating the flint manifest.json
 */
public class GenerateFlintManifestTask extends DefaultTask {
  @OutputFile
  private final File manifestFile;

  private final ManifestStaticFileInput staticFiles;

  @Nested
  private final ManifestPackageDependencyInput packageDependencies;

  @InputFile
  private File artifactURLsCacheFile;

  @InputFile
  private File staticFilesChecksumsCacheFile;

  private FlintGradleExtension extension;

  /**
   * Constructs a new {@link GenerateFlintManifestTask}.
   *
   * @param manifestFile        The file to write the generated manifest to
   * @param staticFiles         The static files to index in the manifest
   * @param packageDependencies The packages the manifest lists as dependencies
   */
  @Inject
  public GenerateFlintManifestTask(
      File manifestFile,
      ManifestStaticFileInput staticFiles,
      ManifestPackageDependencyInput packageDependencies
  ) {
    this.manifestFile = manifestFile;
    this.staticFiles = staticFiles;
    this.packageDependencies = packageDependencies;
  }

  /**
   * Retrieves the file to write the generated manifest to.
   *
   * @return The file to write the generated manifest to
   */
  public File getManifestFile() {
    return manifestFile;
  }

  /**
   * Retrieves the input file this task reads the maven artifact URL's from.
   *
   * @return The file this task reads the maven artifact URL's from
   */
  public File getArtifactURLsCacheFile() {
    if(artifactURLsCacheFile == null) {
      artifactURLsCacheFile = BoundMavenDependencies.getCacheFile(getProject());
    }

    return artifactURLsCacheFile;
  }

  /**
   * Retrieves the input file this task reads the static file checksums from.
   *
   * @return The file this task reads the static file checksums from
   */
  public File getStaticFilesChecksumsCacheFile() {
    if(staticFilesChecksumsCacheFile == null) {
      staticFilesChecksumsCacheFile = StaticFileChecksums.getCacheFile(getProject());
    }

    return staticFilesChecksumsCacheFile;
  }

  /**
   * Retrieves the package dependencies for this manifest.
   *
   * @return The package dependencies for this manifest
   */
  @SuppressWarnings("unused") // Required for @Nested on `packageDependencies`
  public ManifestPackageDependencyInput getPackageDependencies() {
    return packageDependencies;
  }

  /**
   * Retrieves the static files for this manifest.
   *
   * @return The static files for this manifest
   */
  // NOTE: This method is only required for gradle to correctly calculate the up-to-date state of this task
  @Input
  public Set<ManifestStaticFile> getStaticFiles() {
    Set<ManifestStaticFile> out = new HashSet<>();
    out.addAll(staticFiles.getRemoteFiles());
    out.addAll(staticFiles.getLocalFiles().values());

    return out;
  }

  /**
   * Retrieves the group for this manifest.
   *
   * @return The group for this manifest
   */
  @Input
  public String getProjectGroup() {
    return getProject().getGroup().toString();
  }

  /**
   * Retrieves the name for this manifest.
   *
   * @return The name for this manifest
   */
  @Input
  public String getProjectName() {
    return getProject().getName();
  }

  /**
   * Retrieves the description for this manifest.
   *
   * @return The description for this manifest
   */
  @Input
  public String getProjectDescription() {
    String description = getProject().getDescription();
    return description != null ? description : "A flint project";
  }

  /**
   * Retrieves the version for this manifest.
   *
   * @return The version for this manifest
   */
  @Input
  public String getProjectVersion() {
    return getProject().getVersion().toString();
  }

  /**
   * Retrieves the channel name for this manifest.
   *
   * @return The channel name for this manifest
   */
  @Input
  public String getChannel() {
    String channel = FlintPluginProperties.DISTRIBUTOR_CHANNEL.resolve(getProject());
    return channel == null ? "development" : channel;
  }

  /**
   * Retrieves the minecraft versions for this manifest.
   *
   * @return The minecraft versions for this manifest
   */
  @Input
  public Set<String> getMinecraftVersions() {
    return getExtension().getMinecraftVersions();
  }

  /**
   * Retrieves the flint version for this manifest.
   *
   * @return The flint version for this manifest
   */
  @Input
  public String getFlintVersion() {
    return getExtension().getFlintVersion();
  }

  /**
   * Retrieves the authors for this manifest.
   *
   * @return The authors for this manifest
   */
  @Input
  public Set<String> getAuthors() {
    return new HashSet<>(Arrays.asList(getExtension().getAuthors()));
  }

  /**
   * Retrieves the flint extension controlling this manifest.
   *
   * @return The flint extension controlling this manifest
   */
  private FlintGradleExtension getExtension() {
    if(extension == null) {
      extension = getProject().getExtensions().getByType(FlintGradleExtension.class);
    }

    return extension;
  }

  @TaskAction
  public void generate() {
    if(!getArtifactURLsCacheFile().isFile()) {
      throw new IllegalStateException("Missing maven artifacts URLs cache file");
    } else if(!getStaticFilesChecksumsCacheFile().isFile()) {
      throw new IllegalStateException("Missing static file checksum cache file");
    }

    // Load cached artifact URLs
    Map<ManifestMavenDependency, URI> dependencyURIs;
    try {
      dependencyURIs = BoundMavenDependencies.load(artifactURLsCacheFile);
    } catch(IOException e) {
      throw new FlintGradleException("IOException while loading cached maven artifact URLs", e);
    }

    // Load cached checksums
    StaticFileChecksums checksums;
    try {
      checksums = StaticFileChecksums.load(staticFilesChecksumsCacheFile);
    } catch(IOException e) {
      throw new FlintGradleException("IOException while loading cached static files checksums", e);
    }

    // Build package dependencies
    Set<DependencyDescriptionModel> dependencyDescriptionModels = new HashSet<>();
    for(ManifestPackageDependency dependency : packageDependencies.getDependencies()) {
      dependencyDescriptionModels.add(new DependencyDescriptionModel(
          dependency.getName(),
          dependency.getVersion(),
          dependency.getChannel()
      ));
    }

    // Build all install instructions
    Set<InstallInstructionModel> mavenInstallInstructions = buildMavenInstallInstructions(dependencyURIs);
    Set<InstallInstructionModel> staticFileInstallInstructions = buildStaticFileInstructions(checksums);
    InstallInstructionModel ownInstallInstruction = buildOwnInstallInstruction();

    // Build the runtime classpath
    Set<String> runtimeClasspath = new HashSet<>();
    for(InstallInstructionModel mavenInstallInstruction : mavenInstallInstructions) {
      // Add all maven dependencies to the runtime classpath
      DownloadMavenDependencyDataModel data = mavenInstallInstruction.getData();
      if(!data.getPath().equals(ownInstallInstruction.<DownloadMavenDependencyDataModel>getData().getPath())) {
        // If the library does not equal ourself, add it
        runtimeClasspath.add(data.getPath());
      }
    }

    for(InstallInstructionModel staticFileInstallInstruction : staticFileInstallInstructions) {
      DownloadFileDataModel data = staticFileInstallInstruction.getData();
      if(data.getPath().endsWith(".jar")) {
        // If the static file is a jar, add it to the classpath
        runtimeClasspath.add(data.getPath());
      }
    }

    // Collect all install instructions
    List<InstallInstructionModel> allInstallInstructions = new ArrayList<>();
    allInstallInstructions.add(ownInstallInstruction);
    allInstallInstructions.addAll(mavenInstallInstructions);
    allInstallInstructions.addAll(staticFileInstallInstructions);

    // Build package model
    PackageModel model = new PackageModel(
        getProjectGroup(),
        getProjectName(),
        getProjectDescription(),
        getProjectVersion(),
        getChannel(),
        String.join(",", getMinecraftVersions()),
        getFlintVersion(),
        getAuthors(),
        dependencyDescriptionModels,
        runtimeClasspath,
        allInstallInstructions
    );

    // Ensure the manifest file is writeable
    File manifestParentDir = manifestFile.getParentFile();
    if(!manifestParentDir.isDirectory() && !manifestParentDir.mkdirs()) {
      throw new FlintGradleException("Failed to create directory " + manifestParentDir.getAbsolutePath());
    }

    // Write the manifest
    try(OutputStream stream = new FileOutputStream(manifestFile)) {
      JsonConverter.OBJECT_MAPPER.writeValue(stream, model);
    } catch(IOException e) {
      throw new FlintGradleException("Failed to write manifest file", e);
    }
  }

  /**
   * Builds the install instructions required by the maven dependencies.
   *
   * @param mavenDependencyURIs The URI's of the maven dependencies
   * @return The install instructions of the maven dependencies
   */
  private Set<InstallInstructionModel> buildMavenInstallInstructions(
      Map<ManifestMavenDependency, URI> mavenDependencyURIs) {
    Set<InstallInstructionModel> out = new HashSet<>();

    for(Map.Entry<ManifestMavenDependency, URI> entry : mavenDependencyURIs.entrySet()) {
      MavenArtifact artifact = entry.getKey().getArtifact();

      // Construct the path relative to the root of the libraries folder
      String localPath = String.format(
          "${FLINT_LIBRARY_DIR}/%s/%s/%s/%s-%s%s.jar",
          artifact.getGroupId().replace('.', '/'),
          artifact.getArtifactId(),
          artifact.getVersion(),
          artifact.getArtifactId(),
          artifact.getVersion(),
          artifact.getClassifier() == null ? "" : "-" + artifact.getClassifier()
      );

      // Add the instruction
      out.add(new InstallInstructionModel(
          InstallInstructionTypes.DOWNLOAD_MAVEN_DEPENDENCY,
          new DownloadMavenDependencyDataModel(
              artifact.getGroupId(),
              artifact.getArtifactId(),
              artifact.getVersion(),
              artifact.getClassifier(),
              entry.getValue().toASCIIString(),
              localPath
          )
      ));
    }

    return out;
  }

  /**
   * Builds the install instruction required to install the project jar.
   *
   * @return The install instruction of the project jar
   */
  private InstallInstructionModel buildOwnInstallInstruction() {
    String targetPath;

    if(getExtension().getType() == FlintGradleExtension.Type.LIBRARY) {
      // If the jar is a library, put it into the libraries folder
      targetPath = "${FLINT_LIBRARY_DIR}/" +
          getProjectGroup().replace('.', '/') +
          getProjectName() + "/" +
          getProjectVersion() + "/" +
          getProjectName() + "-" + getProjectVersion() + ".jar";
    } else {
      // The jar is a package, put it into the package dir
      targetPath = "${FLINT_PACKAGE_DIR}/" + getProjectName() + ".jar";
    }

    return new InstallInstructionModel(
        InstallInstructionTypes.DOWNLOAD_MAVEN_DEPENDENCY,
        new DownloadMavenDependencyDataModel(
            getProjectGroup(),
            getProjectName(),
            getProjectVersion(),
            "${FLINT_DISTRIBUTOR_URL}" + getChannel(),
            null,
            targetPath
        )
    );
  }

  /**
   * Builds the install instructions required by the static files.
   *
   * @param checksums The cached checksums
   * @return The install instructions of the static files
   */
  private Set<InstallInstructionModel> buildStaticFileInstructions(StaticFileChecksums checksums) {
    Set<InstallInstructionModel> out = new HashSet<>();

    // Process local files
    for(Map.Entry<File, ManifestStaticFile> entry : staticFiles.getLocalFiles().entrySet()) {
      File file = entry.getKey();
      ManifestStaticFile data = entry.getValue();

      if(!checksums.has(file)) {
        // Should not happen unless the user explicitly excluded the checksum calculation task
        throw new IllegalStateException("No cached checksum found for file " + file.getAbsolutePath());
      }

      // Add the instruction
      out.add(new InstallInstructionModel(
          InstallInstructionTypes.DOWNLOAD_FILE,
          new DownloadFileDataModel(
              data.getURI().toASCIIString(),
              data.getPath(),
              checksums.get(file)
          )
      ));
    }

    // Process remote files
    for(ManifestStaticFile remoteFile : staticFiles.getRemoteFiles()) {
      if(!checksums.has(remoteFile.getURI())) {
        // Should not happen unless the user explicitly excluded the checksum calculation task
        throw new IllegalStateException("No cached checksum found for URI " + remoteFile.getURI().toASCIIString());
      }

      // Add the instruction
      out.add(new InstallInstructionModel(
          InstallInstructionTypes.DOWNLOAD_FILE,
          new DownloadFileDataModel(
              remoteFile.getURI().toASCIIString(),
              remoteFile.getPath(),
              checksums.get(remoteFile.getURI())
          )
      ));
    }

    return out;
  }
}
