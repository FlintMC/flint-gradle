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

package net.flintmc.gradle.environment.mcp.function;

import net.flintmc.gradle.environment.DeobfuscationException;
import net.flintmc.gradle.environment.DeobfuscationUtilities;
import net.flintmc.gradle.maven.SimpleMavenRepository;
import net.flintmc.gradle.maven.pom.MavenDependency;
import net.flintmc.gradle.maven.pom.MavenPom;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ListLibrariesFunction extends MCPFunction {
  private final MavenPom clientPom;
  private final List<Path> directLibraries;

  /**
   * Constructs a new ListLibraries function with the given name and output.
   *
   * @param name      The name of the function
   * @param output    The output of the function
   * @param clientPom The POM of the client jar
   */
  public ListLibrariesFunction(String name, Path output, MavenPom clientPom) {
    super(name, output);
    this.clientPom = clientPom;
    this.directLibraries = new ArrayList<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void prepare(DeobfuscationUtilities utilities) throws DeobfuscationException {
    SimpleMavenRepository internalRepository = utilities.getInternalRepository();

    // We only need the direct dependency, transitives ones don't matter
    for (MavenDependency dependency : clientPom.getDependencies()) {
      if (!internalRepository.isInstalled(dependency)) {
        throw new DeobfuscationException(
            "Direct minecraft dependency " + dependency + " is not installed", new IllegalStateException());
      }

      // Index the library
      directLibraries.add(internalRepository.getArtifactPath(dependency));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(DeobfuscationUtilities utilities) throws DeobfuscationException {
    try (PrintStream stream = new PrintStream(Files.newOutputStream(output), false, "UTF-8")) {
      // Iterate all direct library dependency paths
      for (Path libraryPath : directLibraries) {
        // Write out the path
        stream.println("-e=" + libraryPath.toString());
      }
    } catch (IOException e) {
      throw new DeobfuscationException("Failed to write library list file", e);
    }
  }
}
