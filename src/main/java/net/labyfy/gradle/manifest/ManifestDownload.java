package net.labyfy.gradle.manifest;

import java.util.Objects;

/**
 * Java representation for labyfy installer download entry for manifest file.
 */
public class ManifestDownload {

  private static final String type = "DOWNLOAD_FILE";
  private ManifestDownloadData data = new ManifestDownloadData();

  public ManifestDownload() {
  }

  public ManifestDownload(String url, String path, String md5) {
    this.data
        .setUrl(url)
        .setPath(path)
        .setMd5(md5);
  }

  public String getUrl() {
    return data.getUrl();
  }

  public ManifestDownload setUrl(String url) {
    data.setUrl(url);
    return this;
  }

  public String getPath() {
    return data.getPath();
  }

  public ManifestDownload setPath(String path) {
    data.setPath(path);
    return this;
  }

  public String getMd5() {
    return data.getMd5();
  }

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
