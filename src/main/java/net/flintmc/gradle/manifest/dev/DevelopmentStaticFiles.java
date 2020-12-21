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
   * @param source The project which the static files belong to
   * @param path   The path the file will be installed to
   * @param file   The local file to install
   */
  public static void register(Project source, String path, File file) {
    String dependencyNotation = dependencyNotation(source);
    LOCAL_FILES.computeIfAbsent(dependencyNotation, (x) -> new HashMap<>()).put(path, file);
  }

  /**
   * Generates a dependency notation for the given project.
   *
   * @param project The project to generation the dependency notation for
   * @return The generated dependency notation
   */
  private static String dependencyNotation(Project project) {
    return dependencyNotation(project.getGroup().toString(), project.getName(), project.getVersion().toString());
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

    return localFiles.get(path);
  }
}
