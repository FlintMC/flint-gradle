package net.flintmc.gradle.manifest;

import java.util.Objects;

/**
 * Java representation for flint installer download entry for manifest file.
 *
 * @see ManifestDownloadData
 */
public class ManifestDownload {

  private static final String type = "DOWNLOAD_FILE";
  private final ManifestDownloadData data = new ManifestDownloadData();

  public ManifestDownload() {
  }

  /**
   * @see ManifestDownloadData
   */
  public ManifestDownload(String url, String path, String md5) {
    this.data
        .setUrl(url)
        .setPath(path)
        .setMd5(md5);
  }

  /**
   * @see ManifestDownloadData#getUrl()
   */
  public String getUrl() {
    return data.getUrl();
  }

  /**
   * @see ManifestDownloadData#setUrl(String)
   */
  public ManifestDownload setUrl(String url) {
    data.setUrl(url);
    return this;
  }

  /**
   * @see ManifestDownloadData#getPath()
   */
  public String getPath() {
    return data.getPath();
  }

  /**
   * @see ManifestDownloadData#setPath(String)
   */
  public ManifestDownload setPath(String path) {
    data.setPath(path);
    return this;
  }

  /**
   * @see ManifestDownloadData#getMd5()
   */
  public String getMd5() {
    return data.getMd5();
  }

  /**
   * @see ManifestDownloadData#setMd5(String)
   */
  public ManifestDownload setMd5(String md5) {
    data.setMd5(md5);
    return this;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ManifestDownload that = (ManifestDownload) o;
    return Objects.equals(data, that.data);
  }

  public int hashCode() {
    return Objects.hash(type, data);
  }

}
