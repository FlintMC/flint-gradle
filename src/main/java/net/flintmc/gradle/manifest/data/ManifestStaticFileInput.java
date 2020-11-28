package net.flintmc.gradle.manifest.data;

import net.flintmc.gradle.extension.FlintGradleExtension;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Cacheable listing of static file entries.
 */
public class ManifestStaticFileInput {
  private final Set<ManifestStaticFile> staticFiles;
  private final Set<File> localInputs;

  public ManifestStaticFileInput() {
    staticFiles = new HashSet<>();
    this.localInputs = new HashSet<>();
  }

  public void compute(Project project) {
    FlintGradleExtension extension = project.getExtensions().getByType(FlintGradleExtension.class);
  }

  @Input
  public Set<ManifestStaticFile> getStaticFiles() {
    return staticFiles;
  }

  @InputFiles
  public Set<File> getLocalInputs() {
    return localInputs;
  }
}
