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

package net.flintmc.gradle.manifest.cache;

import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.util.Util;
import org.gradle.api.Project;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MavenArtifactChecksums implements Externalizable {
  /**
   * Loads the cached static file checksums from a file.
   *
   * @param file The file to load the cached checksums from
   * @return The loaded checksums
   * @throws IOException If an I/O error occurs while loading the checksums
   */
  public static MavenArtifactChecksums load(File file) throws IOException {
    try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
      return Util.forceCast(in.readObject());
    } catch (ClassNotFoundException e) {
      throw new IOException("Failed to read cached checksums of maven artifacts due to ClassNotFoundException", e);
    }
  }

  private Map<MavenArtifact, String> artifactChecksums;

  /**
   * Constructs a new {@link MavenArtifactChecksums} with all values empty.
   */
  public MavenArtifactChecksums() {
    this.artifactChecksums = new HashMap<>();
  }

  /**
   * Saves this instance to the given file.
   *
   * @param file The file to write the cache to
   * @throws IOException If an I/O error occurs
   */
  public void save(File file) throws IOException {
    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
      out.writeObject(this);
    }
  }

  /**
   * Adds a checksum to the map of URI checksums.
   *
   * @param mavenArtifact The maven artifact to add the checksum for
   * @param checksum      The checksum of the download
   */
  public void add(MavenArtifact mavenArtifact, String checksum) {
    this.artifactChecksums.put(mavenArtifact, checksum);
  }

  /**
   * Checks if the checksums contain a checksum for the given URI.
   *
   * @param mavenArtifact The maven artifact to check for
   * @return {@code true} if a checksum for the URI has been cached, {@code false} otherwise
   */
  public boolean has(MavenArtifact mavenArtifact) {
    return artifactChecksums.containsKey(mavenArtifact);
  }

  /**
   * Retrieves the checksum for the given URI.
   *
   * @param mavenArtifact The maven artifact to retrieve the checksum for
   * @return The checksum of the download behind the URI, or {@code null}, if the URI has not been cached
   */
  public String get(MavenArtifact mavenArtifact) {
    return artifactChecksums.get(mavenArtifact);
  }

  /**
   * Clears all URI checksums.
   */
  public void clearArtifacts() {
    artifactChecksums.clear();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(this.artifactChecksums);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.artifactChecksums = Util.forceCast(in.readObject());
  }

  /**
   * Retrieves a project unique file to cache bound dependencies in.
   *
   * @param project The project to retrieve the cache file for
   * @return The cache file
   */
  public static File getCacheFile(Project project) {
    return new File(Util.getProjectCacheDir(project), "artifact-checksums.bin");
  }
}
