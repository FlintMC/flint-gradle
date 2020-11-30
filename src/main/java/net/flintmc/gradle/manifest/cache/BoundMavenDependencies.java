package net.flintmc.gradle.manifest.cache;

import net.flintmc.gradle.manifest.data.ManifestMavenDependency;
import net.flintmc.gradle.util.Util;
import org.gradle.api.Project;

import java.io.*;
import java.net.URI;
import java.util.Map;

public class BoundMavenDependencies {
  /**
   * Loads the bound dependencies from a file.
   *
   * @param file The file to load the bound dependencies from
   * @return The loaded bound dependencies
   * @throws IOException If an I/O error occurs
   */
  public static Map<ManifestMavenDependency, URI> load(File file) throws IOException {
    try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
      return Util.forceCast(in.readObject());
    } catch(ClassNotFoundException e) {
      throw new IOException("Failed to load cached bound maven dependencies due to ClassNotFoundException", e);
    }
  }

  /**
   * Saves the bound dependencies to a file.
   *
   * @param file         The file to save the bound dependencies to
   * @param dependencies The bound dependencies to save
   * @throws IOException If an I/O error occurs
   */
  public static void save(File file, Map<ManifestMavenDependency, URI> dependencies) throws IOException {
    try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
      out.writeObject(dependencies);
    }
  }

  /**
   * Retrieves a project unique file to cache bound dependencies in.
   *
   * @param project The project to retrieve the cache file for
   * @return The cache file
   */
  public static File getCacheFile(Project project) {
    return new File(Util.getProjectCacheDir(project), "artifact-repositories.bin");
  }
}
