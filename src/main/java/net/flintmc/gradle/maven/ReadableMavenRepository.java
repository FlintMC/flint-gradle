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
