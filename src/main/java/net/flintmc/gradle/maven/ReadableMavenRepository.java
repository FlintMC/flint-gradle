package net.flintmc.gradle.maven;

import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.maven.pom.MavenPom;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Represents a maven repository which artifacts can be read from.
 */
public interface ReadableMavenRepository {
  /**
   * Opens an input stream for the given artifact.
   *
   * @param artifact The artifact to open the stream to
   * @return The opened stream, or {@code null} if the artifact can't be found
   * @throws IOException If an I/O error occurs while opening the input stream
   */
  InputStream getArtifactStream(MavenArtifact artifact) throws IOException;

  /**
   * Retrieves the POM for the given artifact.
   *
   * @param artifact The artifact to retrieve the POM for
   * @return The read POM or {@code null} if there is no POM for the given artifact
   * @throws IOException If an I/O error occurs while reading the POM
   */
  MavenPom getArtifactPom(MavenArtifact artifact) throws IOException;

  /**
   * Retrieves the URI for the given artifact.
   *
   * @param artifact The artifact to retrieve the URI for
   * @return The URI of the artifact or {@code null} if the artifact could not be found
   * @throws IOException If an I/O error occurs while checking if the artifact exists
   */
  URI getArtifactURI(MavenArtifact artifact) throws IOException;
}
