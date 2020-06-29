package net.labyfy.gradle.minecraft.data.version;

import java.nio.file.Path;
import java.util.Objects;

public class VersionedArtifactDownload extends VersionedDownload {
    String id;
    private Path path;

    public String getId() {
        return id;
    }

    public Path getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionedArtifactDownload that = (VersionedArtifactDownload) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, path);
    }
}
