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

import java.util.Objects;

public class MavenDependency extends MavenArtifact {
  private MavenDependencyScope scope;
  private boolean optional;

  /**
   * Constructs a new maven dependency by copying the given one.
   *
   * @param other The maven dependency to copy from
   */
  public MavenDependency(MavenDependency other) {
    super(other);
    this.scope = other.scope;
    this.optional = other.optional;
  }

  /**
   * Constructs a new maven dependency by copying from the given artifact and setting it the given scope.
   *
   * @param artifact The artifact to copy from
   * @param scope    The scope of the dependency
   */
  public MavenDependency(MavenArtifact artifact, MavenDependencyScope scope) {
    super(artifact);
    this.scope = scope;
  }

  /**
   * Constructs a new maven dependency from the given artifact identifier and scope.
   *
   * @param artifactIdentifier The identifier to parse
   * @param scope              The scope of the dependency
   */
  public MavenDependency(String artifactIdentifier, MavenDependencyScope scope) {
    super(artifactIdentifier);
    this.scope = scope;
  }

  /**
   * Constructs a new given maven dependency with the given groupId, artifactId, version and scope.
   *
   * @param groupId    The group id of the artifact
   * @param artifactId The id of the artifact
   * @param version    The version of the artifact
   * @param scope      The scope of the dependency
   */
  public MavenDependency(String groupId, String artifactId, String version, MavenDependencyScope scope) {
    super(groupId, artifactId, version);
    this.scope = scope;
  }

  /**
   * Constructs a new given maven dependency with the given groupId, artifactId, version and classifier.
   *
   * @param groupId    The group id of the artifact
   * @param artifactId The id of the artifact
   * @param version    The version of the artifact
   * @param classifier The classifier of the artifact, or {@code null} if none
   * @param scope      The scope of the dependency
   */
  public MavenDependency(
      String groupId, String artifactId, String version, String classifier, MavenDependencyScope scope) {
    super(groupId, artifactId, version, classifier);
  }

  /**
   * Constructs a new given maven dependency with the given groupId, artifactId, version, classifier and type.
   *
   * @param groupId    The group id of the artifact
   * @param artifactId The id of the artifact
   * @param version    The version of the artifact
   * @param classifier The classifier of the artifact, or {@code null} if none
   * @param type       The type of the artifact, or {@code null} for {@code "jar"}
   * @param scope      The scope of the dependency
   * @param optional   If the dependency is optional
   */
  public MavenDependency(
      String groupId,
      String artifactId,
      String version,
      String classifier,
      String type,
      MavenDependencyScope scope,
      boolean optional
  ) {
    super(groupId, artifactId, version, classifier, type);
    this.scope = scope;
    this.optional = optional;
  }

  /**
   * Retrieves the scope of this maven dependency.
   *
   * @return The scope of this maven dependency
   */
  public MavenDependencyScope getScope() {
    return scope;
  }

  /**
   * Sets the scope of this maven dependency.
   *
   * @param scope The scope of this dependency
   */
  public void setScope(MavenDependencyScope scope) {
    this.scope = scope;
  }

  /**
   * Determines if this maven dependency is optional.
   *
   * @return {@code true} if this dependency is optional, {@code false} otherwise
   */
  public boolean isOptional() {
    return optional;
  }

  /**
   * Sets if this maven dependency is optional
   *
   * @param optional Determines if this as an optional maven dependency
   */
  public void setOptional(boolean optional) {
    this.optional = optional;
  }

  /**
   * Determines if this dependency has broken properties.
   *
   * @return {@code true} if this dependency has broken properties, {@code false} otherwise
   */
  public boolean isBroken() {
    return isInvalid(getGroupId(), false) ||
        isInvalid(getArtifactId(), false) ||
        isInvalid(getVersion(), false) ||
        isInvalid(getClassifier(), true) ||
        isInvalid(getType(), true);
  }

  /**
   * Determines if the given string is invalid as a maven URL part.
   *
   * @param toTest    The string to test if invalid
   * @param allowNull If null values are considered value
   * @return {@code true} if the string is invalid, {@code false} otherwise
   */
  private boolean isInvalid(String toTest, boolean allowNull) {
    return (toTest == null) ? !allowNull :
        toTest.contains("$") || toTest.contains("{") || toTest.contains("}") || toTest.contains("/");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MavenDependency)) return false;
    if (!super.equals(o)) return false;
    MavenDependency that = (MavenDependency) o;
    return scope == that.scope;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), scope);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "MavenDependency{" +
        "scope=" + scope +
        ", SUPER=" + super.toString() +
        "}";
  }
}
