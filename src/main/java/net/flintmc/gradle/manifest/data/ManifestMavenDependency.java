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

package net.flintmc.gradle.manifest.data;

import net.flintmc.gradle.maven.pom.MavenArtifact;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents a maven dependency which can be present in the cached manifest input.
 */
public class ManifestMavenDependency implements Externalizable {
  private MavenArtifact artifact;

  /**
   * Constructs a new {@link ManifestMavenDependency}.
   *
   * @param artifact The maven artifact represented by this dependency
   */
  public ManifestMavenDependency(MavenArtifact artifact) {
    this.artifact = artifact;
  }

  /**
   * Constructs a new {@link ManifestMavenDependency} with all values set to null, used for deserialization.
   */
  public ManifestMavenDependency() {
  }

  /**
   * Retrieves the artifact represented by this dependency.
   *
   * @return The artifact represented by this dependency
   */
  public MavenArtifact getArtifact() {
    return artifact;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeUTF(artifact.getGroupId());
    out.writeUTF(artifact.getArtifactId());
    out.writeUTF(artifact.getVersion());
    out.writeObject(artifact.getClassifier()); // nullable, thus writeObject instead of writeUTF
    out.writeObject(artifact.getType()); // nullable, thus writeObject instead of writeUTF
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    String groupId = in.readUTF();
    String artifactId = in.readUTF();
    String version = in.readUTF();
    String classifier = (String) in.readObject(); // nullable, thus readObject instead of readUTF
    String type = (String) in.readObject(); // nullable, thus readObject instead of readUTF

    this.artifact = new MavenArtifact(
        groupId,
        artifactId,
        version,
        classifier,
        type
    );
  }
}
