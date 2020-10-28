package net.flintmc.gradle.manifest;

/**
 * Java representation for flint installer manifest file.
 */
public class Manifest {
  private String name;
  private String description;
  private String version;
  private String[] authors;
  private ManifestDownload[] downloads;

  public Manifest() {
  }

  /**
   * Constructs a new {@link Manifest}
   *
   * @param Flint       package name
   * @param description Flint package description
   * @param version     Flint package version
   * @param authors     Flint package authors
   * @param downloads   Flint package downloads required for execution
   */
  public Manifest(String name, String description, String version, String[] authors, ManifestDownload[] downloads) {
    this.name = name;
    this.description = description;
    this.version = version;
    this.authors = authors;
    this.downloads = downloads;
  }

  /**
   * @return The Flint package name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the Flint package name.
   *
   * @param name The flint package name
   * @return this
   */
  public Manifest setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * @return The Flint package description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the Flint package description.
   *
   * @param name The flint package description
   * @return this
   */
  public Manifest setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * @return The Flint package version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the Flint package version.
   *
   * @param name The flint package version
   * @return this
   */
  public Manifest setVersion(String version) {
    this.version = version;
    return this;
  }


  /**
   * @return The Flint package authors
   */
  public String[] getAuthors() {
    return authors;
  }

  /**
   * Sets the Flint package authors.
   *
   * @param name The flint package authors
   * @return this
   */
  public Manifest setAuthors(String[] authors) {
    this.authors = authors;
    return this;
  }

  /**
   * @return The Flint package downloads required to be executed
   */
  public ManifestDownload[] getDownloads() {
    return downloads;
  }

  /**
   * Sets the Flint package downloads required to be executed.
   *
   * @param name The flint package downloads
   * @return this
   */
  public Manifest setDownloads(ManifestDownload[] downloads) {
    this.downloads = downloads;
    return this;
  }
}
