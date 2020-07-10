package net.labyfy.gradle.manifest;

public class Manifest {
  private String name;
  private String description;
  private String version;
  private String[] authors;
  private ManifestDownload[] downloads;

  public Manifest() {
  }

  public Manifest(String name, String description, String version, String[] authors, ManifestDownload[] downloads) {
    this.name = name;
    this.description = description;
    this.version = version;
    this.authors = authors;
    this.downloads = downloads;
  }

  public String getName() {
    return name;
  }

  public Manifest setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Manifest setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public Manifest setVersion(String version) {
    this.version = version;
    return this;
  }

  public String[] getAuthors() {
    return authors;
  }

  public Manifest setAuthors(String[] authors) {
    this.authors = authors;
    return this;
  }

  public ManifestDownload[] getDownloads() {
    return downloads;
  }

  public Manifest setDownloads(ManifestDownload[] downloads) {
    this.downloads = downloads;
    return this;
  }
}
