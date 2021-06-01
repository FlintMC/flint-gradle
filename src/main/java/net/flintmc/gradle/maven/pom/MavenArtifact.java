/*
 * FlintMC
 * Copyright (C) 2020-2021 LabyMedia GmbH and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.flintmc.gradle.maven.pom;

import java.io.Serializable;
import java.util.Objects;

public class MavenArtifact implements Serializable {
  private String groupId;
  private String artifactId;
  private String version;
  private String classifier;
  private String type;

  /**
   * Constructs a new maven artifact by copying from the given one.
   *
   * @param other The artifact to copy from
   */
  public MavenArtifact(MavenArtifact other) {
    this.groupId = other.groupId;
    this.artifactId = other.artifactId;
    this.version = other.version;
    this.classifier = other.classifier;
    this.type = other.type;
  }

  /**
   * Constructs a new maven artifact from the given artifact identifier.
   *
   * @param artifactIdentifier The identifier to parse
   */
  public MavenArtifact(String artifactIdentifier) {
    String[] parts = artifactIdentifier.split(":", 4);

    // groupId:artifactId:version:classifier@type
    groupId = parts[0];
    artifactId = parts[1];
    version = parts[2];
    if (parts.length > 3) {
      String remaining = parts[3];
      int indexOfAt = remaining.lastIndexOf('@');

      if (indexOfAt >= 0) {
        classifier = remaining.substring(0, indexOfAt);
        type = remaining.substring(indexOfAt + 1);
      } else {
        classifier = remaining;
        type = null;
      }
    }
  }

  /**
   * Constructs a new given maven artifact with the given groupId, artifactId and version.
   *
   * @param groupId    The group id of the artifact
   * @param artifactId The id of the artifact
   * @param version    The version of the artifact
   */
  public MavenArtifact(String groupId, String artifactId, String version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  /**
   * Constructs a new given maven artifact with the given groupId, artifactId, version and classifier.
   *
   * @param groupId    The group id of the artifact
   * @param artifactId The id of the artifact
   * @param version    The version of the artifact
   * @param classifier The classifier of the artifact, or {@code null} if none
   */
  public MavenArtifact(String groupId, String artifactId, String version, String classifier) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.classifier = classifier;
  }

  /**
   * Constructs a new given maven artifact with the given groupId, artifactId, version, classifier and type.
   *
   * @param groupId    The group id of the artifact
   * @param artifactId The id of the artifact
   * @param version    The version of the artifact
   * @param classifier The classifier of the artifact, or {@code null} if none
   * @param type       The type of the artifact, or {@code null} for {@code "jar"}
   */
  public MavenArtifact(String groupId, String artifactId, String version, String classifier, String type) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.classifier = classifier;
    this.type = type;
  }

  /**
   * Retrieves the group id of the artifact.
   *
   * @return The group id of the artifact
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Sets the group id of the artifact.
   *
   * @param groupId The group id of the artifact
   */
  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  /**
   * Retrieves the id of the artifact.
   *
   * @return The id of the artifact
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * Sets the id of the artifact.
   *
   * @param artifactId The id of the artifact
   */
  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  /**
   * Retrieves the version of the artifact.
   *
   * @return The version of the artifact
   */
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version of the artifact.
   *
   * @param version The version of the artifact
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Retrieves the classifier of the artifact.
   *
   * @return The classifier of the artifact, or {@code null} if none
   */
  public String getClassifier() {
    return classifier;
  }

  /**
   * Sets the classifier of the artifact.
   *
   * @param classifier The classifier of the artifact, or {@code null} if none
   */
  public void setClassifier(String classifier) {
    this.classifier = classifier;
  }

  /**
   * Retrieves the type of the artifact.
   *
   * @return The type of the artifact, or {@code null} for the "jar" default
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type of the artifact.
   *
   * @param type The type of the artifact, or {@code null} for the "jar" default
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Retrieves the {@link String} representation of this artifact.
   *
   * @return The string representation of this artifact
   */
  public String toIdentifier() {
    return groupId + ':' + artifactId + ':' + version +
        (classifier != null ? ':' + classifier : "") +
        (type != null ? '@' + type : "");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MavenArtifact)) return false;
    MavenArtifact artifact = (MavenArtifact) o;
    return groupId.equals(artifact.groupId) &&
        artifactId.equals(artifact.artifactId) &&
        Objects.equals(version, artifact.version) &&
        Objects.equals(classifier, artifact.classifier) &&
        Objects.equals(type, artifact.type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, classifier, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return toIdentifier();
  }
}
