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
