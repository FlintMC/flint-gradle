package net.flintmc.gradle.minecraft.data.version;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.flintmc.gradle.maven.pom.MavenArtifact;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VersionedLibrary {
    private MavenArtifact name;
    private VersionedLibraryDownloads downloads;
    @JsonProperty("extract")
    private Map<String, List<String>> extractRules;
    private Map<String, String> natives;
    private List<VersionedRule> rules;

    public MavenArtifact getName() {
        return name;
    }

    public VersionedLibraryDownloads getDownloads() {
        return downloads;
    }

    public Map<String, List<String>> getExtractRules() {
        return extractRules;
    }

    public Map<String, String> getNatives() {
        return natives;
    }

    public List<VersionedRule> getRules() {
        return rules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionedLibrary that = (VersionedLibrary) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(downloads, that.downloads) &&
            Objects.equals(extractRules, that.extractRules) &&
            Objects.equals(natives, that.natives) &&
            Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, downloads, extractRules, natives, rules);
    }
}
