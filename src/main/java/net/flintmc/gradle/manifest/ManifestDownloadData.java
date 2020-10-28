package net.flintmc.gradle.manifest;

import java.util.Objects;

/**
 * Java representation for flint installer download entry data for manifest file.
 */
public class ManifestDownloadData {
  private String url;
  private String path;
  private String md5;

  /**
   * @return The download url for the content
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the download url for the content.
   *
   * @param url The download url
   * @return this
   */
  public ManifestDownloadData setUrl(String url) {
    this.url = url;
    return this;
  }

  /**
   * @return The relative path from .minecraft to install the file to
   */
  public String getPath() {
    return path;
  }

  /**
   * Set the relative path from .minecraft to install the file to.
   *
   * @param path Relative path to install the file to
   * @return this
   */
  public ManifestDownloadData setPath(String path) {
    this.path = path;
    return this;
  }

  public String getMd5() {
    return md5;
  }

  /**
   * Sets the md5 hash of the content
   *
   * @param md5 md5 hash of the content
   * @return this
   */
  public ManifestDownloadData setMd5(String md5) {
    this.md5 = md5;
    return this;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ManifestDownloadData data = (ManifestDownloadData) o;
    return Objects.equals(url, data.url) &&
        Objects.equals(path, data.path) &&
        Objects.equals(md5, data.md5);
  }

  public int hashCode() {
    return Objects.hash(url, path, md5);
  }
}
