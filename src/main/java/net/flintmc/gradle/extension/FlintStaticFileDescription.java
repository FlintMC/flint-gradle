package net.flintmc.gradle.extension;

import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entry in the container of the static files block.
 *
 * @see FlintStaticFilesExtension
 */
public class FlintStaticFileDescription {
  private static final Logger LOGGER = Logging.getLogger(FlintStaticFileDescription.class);

  private final Project project;
  private final String name;

  private URI sourceURI;
  private File sourceFile;
  private String target;

  /**
   * Constructs a new {@link FlintStaticFileDescription}.
   *
   * @param project The project this description belongs to
   * @param name    The name of the file
   */
  public FlintStaticFileDescription(Project project, String name) {
    this.project = project;
    this.name = name;
  }

  /**
   * Sets the source this static file should be obtained from.
   *
   * @param source The source of this static file
   */
  public void from(Object source) {
    if(source instanceof URI) {
      from((URI) source);
    } else {
      from(project.file(source).toURI());
    }
  }

  /**
   * Sets the source as an URI this static file should be obtained from.
   *
   * @param uri The source URI of this static file
   */
  public void from(URI uri) {
    switch(uri.getScheme()) {
      case "http":
        LOGGER.warn("Static file {} uses unsafe http scheme", name);
        // fallthrough

      case "https":
        this.sourceURI = uri;
        break;

      case "file":
        this.sourceFile = new File(uri);
        break;

      default:
        throw new IllegalArgumentException(
            "Unsupported URI scheme " + uri.getScheme() + ", only https:// and file:// are supported");
    }
  }

  /**
   * Sets the relative target path of this static file.
   *
   * @param target The target path
   */
  public void to(Object target) {
    this.target = target.toString();
  }

  /**
   * Sets the relative target directory of this static file, the name is used as the file name inside the directory.
   *
   * @param target The target directory path
   */
  public void into(Object target) {
    this.target = target.toString() + "/" + name;
  }

  /**
   * Determines if the file is a remote file.
   *
   * @return {@code true} if the file is a remote file, {@code false} otherwise
   */
  public boolean isRemote() {
    return this.sourceFile == null;
  }

  /**
   * Checks this description for validity.
   *
   * @throws IllegalStateException If this description is not valid
   */
  public void validate() {
    if(this.sourceFile == null && this.sourceURI == null) {
      throw new IllegalStateException("Missing source, from() has not been called");
    }

    if(this.target == null) {
      throw new IllegalStateException("Missing target, to() or into() has not been called");
    }

    try {
      Path targetPath = Paths.get(this.target);
      if(targetPath.isAbsolute()) {
        throw new IllegalStateException("Expected the target path to be a relative path");
      }
    } catch(InvalidPathException e) {
      throw new IllegalStateException("Target path is invalid", e);
    }
  }
}
