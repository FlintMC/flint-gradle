package net.labyfy.gradle.manifest;

import java.util.Objects;

/**
 * Java representation for labyfy installer download entry data for manifest file.
 */
public class ManifestDownloadData {
  private String url;
  private String path;
  private String md5;

  public String getUrl() {
    return url;
  }

  public ManifestDownloadData setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getPath() {
    return path;
  }

  public ManifestDownloadData setPath(String path) {
    this.path = path;
    return this;
  }

  public String getMd5() {
    return md5;
  }

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
