package net.flintmc.gradle.util.resource;

import java.io.File;
import java.util.Collection;

/**
 * Helper class to load resources from a classpath without using a class loader.
 */
public class ResourceLoader {
  private final Collection<File> files;

  /**
   * Constructs a new resource loader.
   *
   * @param files The files to treat as classpath
   */
  public ResourceLoader(Collection<File> files) {
    this.files = files;
  }

  /**
   * Finds all resource with a certain name.
   *
   * @param resourceName The name of the resource to find in the class path
   * @return A searcher searching the classpath for the resource
   */
  public ResourceFinder findAll(String resourceName) {
    return new ResourceFinder(resourceName, files);
  }
}
