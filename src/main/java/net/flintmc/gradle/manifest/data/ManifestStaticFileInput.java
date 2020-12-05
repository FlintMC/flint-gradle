package net.flintmc.gradle.manifest.data;

import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.extension.FlintStaticFileDescription;
import net.flintmc.gradle.manifest.ManifestConfigurator;
import net.flintmc.gradle.util.Util;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.tasks.Internal;

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Cacheable listing of static file entries.
 */
public class ManifestStaticFileInput {
  @Internal
  private final Set<ManifestStaticFile> remoteFiles;

  @Internal
  private final Map<File, ManifestStaticFile> localFiles;

  public ManifestStaticFileInput() {
    remoteFiles = new HashSet<>();
    this.localFiles = new HashMap<>();
  }

  /**
   * Computes the static files for the manifest input.
   *
   * @param project      The project to compute the files for
   * @param configurator The configurator currently computing the inputs
   */
  public void compute(Project project, ManifestConfigurator configurator) {
    NamedDomainObjectContainer<FlintStaticFileDescription> staticFileDescriptions =
        project.getExtensions().getByType(FlintGradleExtension.class).getStaticFiles().getStaticFileDescriptions();

    for(FlintStaticFileDescription staticFileDescription : staticFileDescriptions) {
      staticFileDescription.validate();

      String targetPath = staticFileDescription.getTarget();
      if(staticFileDescription.isRemote()) {
        // Remote file, skip checksum computation
        // we assume remote files only change when their URL changes
        remoteFiles.add(new ManifestStaticFile(
            staticFileDescription.getSourceURI(),
            targetPath
        ));
      } else {
        // Local file, generate an URL
        URI remoteURI = generateURIForLocalFile(staticFileDescription.getName(), configurator);
        localFiles.put(staticFileDescription.getSourceFile(), new ManifestStaticFile(remoteURI, targetPath));
      }
    }
  }

  /**
   * Generates the distributor URL for a static file entry.
   *
   * @param name         The name of the static file
   * @param configurator The manifest configurator currently computing the inputs
   * @return The generated URI
   */
  private URI generateURIForLocalFile(String name, ManifestConfigurator configurator) {
    URI projectMavenURI = configurator.getProjectMavenURI(
        "Remove static file entries, a distributor is URL is required to use static files");

    return Util.concatURI(projectMavenURI, name);
  }

  public Set<ManifestStaticFile> getRemoteFiles() {
    return remoteFiles;
  }

  public Map<File, ManifestStaticFile> getLocalFiles() {
    return localFiles;
  }
}
