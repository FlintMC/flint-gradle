package net.labyfy.gradle.maven.pom;

import java.util.*;

public class MavenPom extends MavenArtifact {
    private final Set<MavenDependency> dependencies;

    /**
     * Constructs a new maven POM by copying from the given one.
     *
     * @param other The maven POM to copy from
     */
    public MavenPom(MavenPom other) {
        super(other);
        this.dependencies = new HashSet<>(other.dependencies);
    }

    /**
     * Constructs a new maven POM by copying from the given artifact.
     * The dependencies will be initially empty.
     *
     * @param other The maven artifact to copy from
     */
    public MavenPom(MavenArtifact other) {
        super(other);
        this.dependencies = new HashSet<>();
    }

    /**
     * Constructs a new maven POM from the given artifact identifier.
     *
     * @param artifactIdentifier The identifier to parse
     */
    public MavenPom(String artifactIdentifier) {
        super(artifactIdentifier);
        this.dependencies = new HashSet<>();
    }

    /**
     * Constructs a new given maven POM with the given groupId, artifactId and version.
     *
     * @param groupId The group id of the artifact
     * @param artifactId The id of the artifact
     * @param version The version of the artifact
     */
    public MavenPom(String groupId, String artifactId, String version) {
        super(groupId, artifactId, version);
        this.dependencies = new HashSet<>();
    }

    /**
     * Constructs a new given maven POM with the given groupId, artifactId, version and classifier.
     *
     * @param groupId The group id of the artifact
     * @param artifactId The id of the artifact
     * @param version The version of the artifact
     * @param classifier The classifier of the artifact, or {@code null} if none
     */
    public MavenPom(String groupId, String artifactId, String version, String classifier) {
        super(groupId, artifactId, version, classifier);
        this.dependencies = new HashSet<>();
    }

    /**
     * Constructs a new given maven POM with the given groupId, artifactId, version, classifier and type.
     *
     * @param groupId The group id of the artifact
     * @param artifactId The id of the artifact
     * @param version The version of the artifact
     * @param classifier The classifier of the artifact, or {@code null} if none
     * @param type The type of the artifact, or {@code null} for {@code "jar"}
     */
    public MavenPom(String groupId, String artifactId, String version, String classifier, String type) {
        super(groupId, artifactId, version, classifier, type);
        this.dependencies = new HashSet<>();
    }

    /**
     * Retrieves the dependency entries of this POM.
     *
     * @return The dependency entries of this POM
     */
    public Set<MavenDependency> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    /**
     * Adds a dependency to this POM.
     *
     * @param artifact The dependency to add
     */
    public void addDependency(MavenDependency artifact) {
        this.dependencies.add(artifact);
    }

    /**
     * Adds the given dependencies to this POM.
     *
     * @param dependencies The dependencies to add
     */
    public void addDependencies(Collection<MavenDependency> dependencies) {
        this.dependencies.addAll(dependencies);
    }

    /**
     * Removes the given dependency from this POM.
     *
     * @param artifact The dependency to remove
     */
    public void removeDependency(MavenDependency artifact) {
        this.dependencies.remove(artifact);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MavenPom)) return false;
        if (!super.equals(o)) return false;
        MavenPom mavenPom = (MavenPom) o;
        return dependencies.equals(mavenPom.dependencies);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), dependencies);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "MavenPom{" +
                "dependencies=" + dependencies +
                ", SUPER= " + super.toString() + "}";
    }
}
