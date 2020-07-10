package net.labyfy.gradle.manifest;

import java.util.Objects;

public class ManifestDownload {

  private final String type = "DOWNLOAD_FILE";
  private Data data = new Data();

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

  public static class Data {
    private String url;
    private String path;
    private String md5;

    public String getUrl() {
      return url;
    }

    public Data setUrl(String url) {
      this.url = url;
      return this;
    }

    public String getPath() {
      return path;
    }

    public Data setPath(String path) {
      this.path = path;
      return this;
    }

    public String getMd5() {
      return md5;
    }

    public Data setMd5(String md5) {
      this.md5 = md5;
      return this;
    }

    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Data data = (Data) o;
      return Objects.equals(url, data.url) &&
          Objects.equals(path, data.path) &&
          Objects.equals(md5, data.md5);
    }

    public int hashCode() {
      return Objects.hash(url, path, md5);
    }
  }
}
