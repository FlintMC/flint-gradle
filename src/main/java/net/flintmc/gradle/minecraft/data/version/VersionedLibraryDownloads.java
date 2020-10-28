package net.flintmc.gradle.minecraft.data.version;

import java.util.Map;
import java.util.Objects;

public class VersionedLibraryDownloads {
    private VersionedArtifactDownload artifact;
    private Map<String, VersionedArtifactDownload> classifiers;

    public VersionedArtifactDownload getArtifact() {
        return artifact;
    }

    public Map<String, VersionedArtifactDownload> getClassifiers() {
        return classifiers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionedLibraryDownloads that = (VersionedLibraryDownloads) o;
        return Objects.equals(artifact, that.artifact) &&
            Objects.equals(classifiers, that.classifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artifact, classifiers);
    }
}
