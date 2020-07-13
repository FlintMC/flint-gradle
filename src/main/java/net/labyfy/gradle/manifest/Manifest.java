package net.labyfy.gradle.manifest;

/**
 * Java representation for labyfy installer manifest file.
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
   * @param Labyfy      package name
   * @param description Labyfy package description
   * @param version     Labyfy package version
   * @param authors     Labyfy package authors
   * @param downloads   Labyfy package downloads required for execution
   */
  public Manifest(String name, String description, String version, String[] authors, ManifestDownload[] downloads) {
    this.name = name;
    this.description = description;
    this.version = version;
    this.authors = authors;
    this.downloads = downloads;
  }

  /**
   * @return The Labyfy package name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the Labyfy package name.
   *
   * @param name The labyfy package name
   * @return this
   */
  public Manifest setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * @return The Labyfy package description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the Labyfy package description.
   *
   * @param name The labyfy package description
   * @return this
   */
  public Manifest setDescription(String description) {
    this.description = description;
    return this;
  }

  /**
   * @return The Labyfy package version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the Labyfy package version.
   *
   * @param name The labyfy package version
   * @return this
   */
  public Manifest setVersion(String version) {
    this.version = version;
    return this;
  }


  /**
   * @return The Labyfy package authors
   */
  public String[] getAuthors() {
    return authors;
  }

  /**
   * Sets the Labyfy package authors.
   *
   * @param name The labyfy package authors
   * @return this
   */
  public Manifest setAuthors(String[] authors) {
    this.authors = authors;
    return this;
  }

  /**
   * @return The Labyfy package downloads required to be executed
   */
  public ManifestDownload[] getDownloads() {
    return downloads;
  }

  /**
   * Sets the Labyfy package downloads required to be executed.
   *
   * @param name The labyfy package downloads
   * @return this
   */
  public Manifest setDownloads(ManifestDownload[] downloads) {
    this.downloads = downloads;
    return this;
  }
}
