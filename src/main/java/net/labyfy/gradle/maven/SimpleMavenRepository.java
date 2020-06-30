package net.labyfy.gradle.maven;

import net.labyfy.gradle.maven.pom.MavenArtifact;
import net.labyfy.gradle.maven.pom.MavenPom;
import net.labyfy.gradle.maven.pom.io.PomReader;
import net.labyfy.gradle.maven.pom.io.PomWriter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Represents a simple, local maven repository.
 */
public class SimpleMavenRepository implements ReadableMavenRepository {
    protected final Path baseDir;

    /**
     * Constructs a new simple maven repository.
     *
     * @param baseDir The root directory of this maven repository
     * @throws IOException If an I/O error occurs while creating the base directory
     */
    public SimpleMavenRepository(Path baseDir) throws IOException {
        if(!Files.isDirectory(baseDir)) {
            Files.createDirectories(baseDir);
        }

        this.baseDir = baseDir;
    }

    /**
     * Retrieves the directory for an artifact.
     *
     * @param group   The group of the artifact
     * @param name    The name of the artifact
     * @param version The version of the artifact
     * @return The path to the directory of the artifact
     */
    public Path getArtifactDirPath(String group, String name, String version) {
        return this.baseDir
                .resolve(group.replace('.', '/'))
                .resolve(name)
                .resolve(version);
    }

    /**
     * Retrieves the path for an artifact.
     *
     * @param group   The group of the artifact
     * @param name    The name of the artifact
     * @param version The version of the artifact
     * @return The path to the artifact
     */
    public Path getArtifactPath(String group, String name, String version) {
        return getArtifactDirPath(group, name, version).resolve(name + '-' + version + ".jar");
    }

    /**
     * Retrieves the path for an artifact.
     *
     * @param group      The group of the artifact
     * @param name       The name of the artifact
     * @param version    The version of the artifact
     * @param classifier The classifier of the artifact
     * @return The path to the artifact
     */
    public Path getArtifactPath(String group, String name, String version, String classifier) {
        return getArtifactDirPath(group, name, version).resolve(name + '-' + version +
                (classifier == null ? "" : "-" + classifier) + ".jar");
    }

    /**
     * Retrieves the path for an artifact.
     *
     * @param group      The group of the artifact
     * @param name       The name of the artifact
     * @param version    The version of the artifact
     * @param classifier The classifier of the artifact
     * @param extension  The extension of the artifact (without the '.')
     * @return The path to the artifact
     */
    public Path getArtifactPath(String group, String name, String version, String classifier, String extension) {
        return getArtifactDirPath(group, name, version).resolve(name + '-' + version +
                (classifier == null ? "" : "-" + classifier) + "." + (extension == null ? "jar" : extension));
    }

    /**
     * Retrieves the path for an artifact.
     *
     * @param artifact The artifact to retrieve the path for
     * @return The path to the artifact
     */
    public Path getArtifactPath(MavenArtifact artifact) {
        return getArtifactPath(
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getVersion(),
                artifact.getClassifier(),
                artifact.getType()
        );
    }

    /**
     * Retrieves the path for a POM file.
     *
     * @param group The group of the artifact the POM file describes
     * @param name The name of the artifact the POM file describes
     * @param version The version of the artifact the POM file describes
     * @return The path to the POM file
     */
    public Path getPomPath(String group, String name, String version) {
        return getPomPath(group, name, version, null);
    }

    /**
     * Retrieves the path for a POM file.
     *
     * @param group The group of the artifact the POM file describes
     * @param name The name of the artifact the POM file describes
     * @param version The version of the artifact the POM file describes
     * @param classifier The classifier of the artifact the POM file describes
     * @return The path to the POM file
     */
    public Path getPomPath(String group, String name, String version, String classifier) {
        return getArtifactDirPath(group, name, version).resolve(name + '-' + version +
                (classifier == null ? "" : "-" + classifier) + ".pom");
    }

    /**
     * Retrieves the path for a POM file.
     *
     * @param artifact The artifact the POM file describes
     * @return The path to the POM file
     */
    public Path getPomPath(MavenArtifact artifact) {
        return getPomPath(
                artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(), artifact.getClassifier());
    }

    /**
     * Writes the given POM into the repository, replacing existing ones.
     *
     * @param pom The POM to write into the repository
     * @throws IOException If an I/O error occurs while writing the POM file
     */
    public void addPom(MavenPom pom) throws IOException {
        Path targetPath = getPomPath(pom);
        PomWriter.write(pom, targetPath);
    }

    /**
     * Determines if the given artifact is installed in this repository.
     *
     * @param artifact The artifact to check for
     * @return {@code true} if the artifact is installed already, {@code false} otherwise
     */
    public boolean isInstalled(MavenArtifact artifact) {
        return Files.isRegularFile(getArtifactPath(artifact));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getArtifactStream(MavenArtifact artifact) throws IOException {
        Path artifactPath = getArtifactPath(artifact);
        return Files.exists(artifactPath) ? Files.newInputStream(artifactPath) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MavenPom getArtifactPom(MavenArtifact artifact) throws IOException {
        Path pomPath = getPomPath(artifact);
        return Files.exists(pomPath) ? PomReader.read(pomPath) : null;
    }
}
