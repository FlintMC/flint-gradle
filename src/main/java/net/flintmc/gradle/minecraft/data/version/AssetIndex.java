package net.flintmc.gradle.minecraft.data.version;

import java.net.URI;
import java.util.Objects;

public class AssetIndex {
  private String id;
  private String sha1;
  private long size;
  private long totalSize;
  private URI url;

  public String getId() {
    return id;
  }

  public String getSha1() {
    return sha1;
  }

  public long getSize() {
    return size;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public URI getUrl() {
    return url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AssetIndex that = (AssetIndex) o;
    return size == that.size &&
        totalSize == that.totalSize &&
        Objects.equals(id, that.id) &&
        Objects.equals(sha1, that.sha1) &&
        Objects.equals(url, that.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sha1, size, totalSize, url);
  }
}
