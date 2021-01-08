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

package net.flintmc.gradle.environment;

import net.flintmc.gradle.environment.mcp.ModCoderPackEnvironment;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.maven.pom.MavenPom;
import net.flintmc.gradle.minecraft.data.environment.EnvironmentInput;

import java.util.Collection;

/**
 * Represents a deobfuscation environment which can be used to obtain the minecraft source.
 */
public interface DeobfuscationEnvironment {
  static DeobfuscationEnvironment createFor(EnvironmentInput input) {
    return new ModCoderPackEnvironment(input.getModCoderPack());
  }

  /**
   * Retrieves the name of the environment. Needs to be filename friendly.
   *
   * @return The name of the environment
   */
  String name();

  /**
   * Runs the deobfuscation on the given client and server artifacts. One of the 2 artifacts may be null, but never both
   * at the same time.
   *
   * @param clientPom The client artifact, may be null if serverPom is not null
   * @param serverPom The client artifact, may be null if clientPom is not null
   * @param utilities Various utilities useful during deobfuscation
   * @throws DeobfuscationException If the deobfuscation fails
   */
  void runDeobfuscation(MavenPom clientPom, MavenPom serverPom, DeobfuscationUtilities utilities)
      throws DeobfuscationException;

  /**
   * Retrieves a collection of all artifacts which should be added to the compile classpath.
   *
   * @param client The client artifact, may be null
   * @param server The server artifact, may be null
   * @return A collection of artifacts which should be added to the compile classpath
   */
  Collection<MavenArtifact> getCompileArtifacts(MavenArtifact client, MavenArtifact server);

  /**
   * Retrieves a collection of all artifacts which should be added to the runtime classpath.
   *
   * @param client The client artifact, may be null
   * @param server The server artifact, may be null
   * @return A collection of artifacts which should be added to the runtime classpath
   */
  Collection<MavenArtifact> getRuntimeArtifacts(MavenArtifact client, MavenArtifact server);
}
