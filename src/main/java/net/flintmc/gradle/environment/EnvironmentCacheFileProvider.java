package net.flintmc.gradle.environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Class for providing a cache for environments.
 */
public class EnvironmentCacheFileProvider {
  private final Path basePath;

  /**
   * Constructs a new {@link EnvironmentCacheFileProvider} with the given base path.
   *
   * @param basePath The base path of this provider
   */
  public EnvironmentCacheFileProvider(Path basePath) {
    this.basePath = basePath;
  }

  /**
   * Retrieves the path for the cached file with the given name.
   *
   * @param name The name of the cached file
   * @return The path to the cached file
   * @throws DeobfuscationException If the parent directories can't be created
   */
  public Path file(String name) throws DeobfuscationException {
    Path filePath = basePath.resolve(name);
    ensureDirExists(filePath.getParent());
    return filePath;
  }

  /**
   * Retrieves the path for the cached directory with the given name.
   *
   * @param name The name of the cached directory
   * @return The path to the cached directory
   * @throws DeobfuscationException If the directory can't be created
   */
  public Path directory(String name) throws DeobfuscationException {
    return ensureDirExists(basePath.resolve(name));
  }

  /**
   * Ensures that the parents of the given paths exist.
   *
   * @param path The path to ensure the parents for
   * @return The path
   * @throws DeobfuscationException If the parent directories can't be created
   */
  private Path ensureDirExists(Path path) throws DeobfuscationException {
    if (!Files.isDirectory(path.getParent())) {
      try {
        Files.createDirectories(path.getParent());
      } catch (IOException e) {
        throw new DeobfuscationException("Failed to create parent directories for cache", e);
      }
    }

    return path;
  }
}
