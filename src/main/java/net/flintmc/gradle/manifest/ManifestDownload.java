package net.flintmc.gradle.manifest;

import java.util.Objects;

/**
 * Java representation for flint installer download entry for manifest file.
 *
 * @see ManifestDownloadData
 */
public class ManifestDownload {

  private static final String type = "DOWNLOAD_FILE";
  private String url;
  private String path;
  private String md5;

  public ManifestDownload() {
  }

  /**
   * @see ManifestDownloadData
   */
  public ManifestDownload(String url, String path, String md5) {
    this.url = url;
    this.path = path;
    this.md5 = md5;
  }

  /**
   * @see ManifestDownloadData#getUrl()
   */
  public String getUrl() {
    return this.url;
  }

  /**
   * @see ManifestDownloadData#setUrl(String)
   */
  public ManifestDownload setUrl(String url) {
    this.url = url;
    return this;
  }

  /**
   * @see ManifestDownloadData#getPath()
   */
  public String getPath() {
    return this.path;
  }

  /**
   * @see ManifestDownloadData#setPath(String)
   */
  public ManifestDownload setPath(String path) {
    this.path = path;
    return this;
  }

  /**
   * @see ManifestDownloadData#getMd5()
   */
  public String getMd5() {
    return this.md5;
  }

  /**
   * @see ManifestDownloadData#setMd5(String)
   */
  public ManifestDownload setMd5(String md5) {
    this.md5 = md5;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ManifestDownload that = (ManifestDownload) o;
    return Objects.equals(url, that.url) && Objects.equals(path, that.path) && Objects.equals(md5, that.md5);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, path, md5);
  }

  @Override
  public String toString() {
    return "ManifestDownload{" +
        "url='" + url + '\'' +
        ", path='" + path + '\'' +
        ", md5='" + md5 + '\'' +
        '}';
  }
}
