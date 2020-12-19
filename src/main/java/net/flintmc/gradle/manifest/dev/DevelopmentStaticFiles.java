package net.flintmc.gradle.manifest.dev;

import net.flintmc.gradle.manifest.data.ManifestStaticFile;
import net.flintmc.gradle.manifest.data.ManifestStaticFileInput;
import org.gradle.api.Project;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for all local static files are registered during build. Provides support for composite builds and static
 * files from project dependencies.
 */
public class DevelopmentStaticFiles {
  private static final Map<String, Map<String, File>> LOCAL_FILES = new HashMap<>();

  /**
   * Registers a project and its static files.
   *
   * @param source          The project which the static files belong to
   * @param staticFileInput The static files to register, they must have been computed already
   */
  public static void register(Project source, ManifestStaticFileInput staticFileInput) {
    // Register the project based on its dependency notation
    String dependencyNotation = dependencyNotation(source);
    if(LOCAL_FILES.containsKey(dependencyNotation)) {
      throw new IllegalStateException("There are already static files registered for the project " + source + ", " +
          "project might be in the build twice or some projects have duplicate names");
    }

    LOCAL_FILES.put(dependencyNotation, new HashMap<>());

    for(Map.Entry<File, ManifestStaticFile> entry : staticFileInput.getLocalFiles().entrySet()) {
      // Map the local path to the file
      File file = entry.getKey();
      ManifestStaticFile description = entry.getValue();

      LOCAL_FILES.get(dependencyNotation).put(description.getPath(), file);
    }
  }

  /**
   * Generates a dependency notation for the given project.
   *
   * @param project The project to generation the dependency notation for
   * @return The generated dependency notation
   */
  private static String dependencyNotation(Project project) {
    return dependencyNotation(project.getName(), project.getGroup().toString(), project.getVersion().toString());
  }

  /**
   * Generates a dependency notation for the given dependency parts.
   *
   * @param group   The group of the dependency
   * @param name    The name of the dependency
   * @param version The version of the dependency
   * @return The generated dependency notation
   */
  private static String dependencyNotation(String group, String name, String version) {
    return group + ":" + name + ":" + version;
  }

  /**
   * Retrieves a static file associated with a given path for the given dependency.
   *
   * @param group   The group of the dependency
   * @param name    The name of the dependency
   * @param version The version of the dependency
   * @param path    The path of the static file
   * @return The local static file, or {@code null}, if the package containing the file is not part of this build
   */
  public static File getFor(String group, String name, String version, String path) {
    String dependencyNotation = dependencyNotation(group, name, version);
    Map<String, File> localFiles = LOCAL_FILES.get(dependencyNotation);
    if(localFiles == null) {
      return null;
    }

    File file = localFiles.get(path);
    if(file == null) {
      // Sanity check, hopefully no-op
      throw new IllegalStateException("Manifest for " + dependencyNotation + " required a static, local file for path " +
          path + ", but the project in the build did not add such file");
    }

    return file;
  }
}
