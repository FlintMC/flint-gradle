package net.flintmc.gradle.minecraft.data.version;

import java.net.URL;
import java.util.Objects;

public class VersionedDownload {
    private String sha1;
    private long size;
    private URL url;

    public String getSha1() {
        return sha1;
    }

    public long getSize() {
        return size;
    }

    public URL getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionedDownload that = (VersionedDownload) o;
        return size == that.size &&
            Objects.equals(sha1, that.sha1) &&
            Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sha1, size, url);
    }
}
