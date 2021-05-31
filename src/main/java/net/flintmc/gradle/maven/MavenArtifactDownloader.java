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
import net.flintmc.gradle.maven.pom.MavenDependency;
import net.flintmc.gradle.maven.pom.MavenDependencyScope;
import net.flintmc.gradle.maven.pom.MavenPom;
import net.flintmc.gradle.maven.pom.io.PomReader;
import net.flintmc.gradle.maven.pom.io.PomWriter;
import net.flintmc.gradle.util.Pair;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for transferring files from remote to local maven repositories.
 */
public class MavenArtifactDownloader {
  private static final Logger LOGGER = Logging.getLogger(MavenArtifactDownloader.class);

  private final List<ReadableMavenRepository> sources;

  /**
   * Constructs a new {@link MavenArtifactDownloader} without any repositories
   */
  public MavenArtifactDownloader() {
    this.sources = new ArrayList<>();
  }

  /**
   * Adds the given repository as a source to this downloader.
   *
   * @param source The repository to add
   */
  public void addSource(ReadableMavenRepository source) {
    this.sources.add(source);
  }

  /**
   * Tests if this downloader has the given repository set as a source.
   *
   * @param source The repository to check if it exists as a source
   * @return {@code true} if this downloader has the give repository as a source, {@code false} otherwise
   */
  public boolean hasSource(ReadableMavenRepository source) {
    return this.sources.contains(source);
  }

  /**
   * Removes the given repository as a source.
   *
   * @param source The repository to remove
   */
  public void removeSource(ReadableMavenRepository source) {
    this.sources.remove(source);
  }

  /**
   * Installs the given artifact with all dependencies into the given repository.
   *
   * @param artifact           The artifact to install
   * @param target             The repository to install the artifact into
   * @param installIfNotExists If the given artifact should also be installed if it does not exist
   * @throws IOException           If an I/O error occurs while installing the artifact or one if its dependencies
   * @throws MavenResolveException If the artifact  or one of its dependencies can't be resolved
   */
  public void installAll(MavenArtifact artifact, SimpleMavenRepository target, boolean installIfNotExists)
      throws IOException, MavenResolveException {
    // Get the local POM path
    Path localPomPath = target.getPomPath(artifact);

    MavenPom artifactPom;
    if(artifact instanceof MavenPom) {
      // If the artifact is a POM already, don't try to read it from anywhere
      artifactPom = (MavenPom) artifact;
    } else {
      // Query the local POM if it exists, else find the POM online
      artifactPom = Files.exists(localPomPath) ? PomReader.read(localPomPath) : findPom(artifact);
    }

    if(artifactPom != null) {
      // POM found, install it with all its dependencies
      installAll(artifactPom, target);

      if(installIfNotExists) {
        if(!Files.exists(localPomPath)) {
          // The POM file does not exist locally, write it down
          PomWriter.write(artifactPom, localPomPath);
        }
      }
    }

    if(installIfNotExists) {
      // If the artifact is not installed locally, try to install it
      if(!target.isInstalled(artifact) && !installArtifact(artifact, target) && artifactPom == null) {
        // The artifact failed to install and there was also no POM for it
        throw new MavenResolveException("Could not resolve " + artifact);
      }
    }
  }

  /**
   * Installs all dependencies recursively required by the given POM into the given repository. This method does not
   * create the POM file if it does not exist.
   *
   * @param pom    The POM to install all artifacts for
   * @param target The repository to install artifacts and POM's into
   * @throws IOException           If an I/O error occurs while installing artifacts
   * @throws MavenResolveException If a dependency fails to resolve
   */
  private void installAll(MavenPom pom, SimpleMavenRepository target) throws IOException, MavenResolveException {
    // Keep track of what should be processed and what has been processed already
    Set<MavenDependency> dependencies = new HashSet<>();
    Set<MavenDependency> toProcess = new HashSet<>(pom.getDependencies());

    // Process until the list to process is empty
    while(!toProcess.isEmpty()) {
      // Copy over the dependencies to process into the current processing round
      Set<MavenDependency> roundDependencies = new HashSet<>(toProcess);
      toProcess.clear();

      for(MavenDependency dependency : roundDependencies) {
        // Mark the dependency as found
        dependencies.add(dependency);

        // Sort out unwanted dependencies
        if(shouldSkip(dependency)) {
          continue;
        } else if(dependency.isBroken()) {
          continue;
        }

        // Resolve the local POM path
        Path localPomPath = target.getPomPath(dependency);

        // Try to read the POM locally, if it does not exist fall back to reading it online
        MavenPom dependencyPom = Files.exists(localPomPath) ? PomReader.read(localPomPath) : findPom(dependency);
        if(dependencyPom != null) {
          // A POM file was found, iterate its dependencies
          for(MavenDependency innerDependency : dependencyPom.getDependencies()) {
            // Sort out unwanted dependencies
            if(shouldSkip(innerDependency)) {
              continue;
            } else if(innerDependency.isBroken()) {
              continue;
            }

            if(!dependencies.contains(innerDependency)) {
              // The dependency has not been found already, process it
              toProcess.add(innerDependency);
            }
          }

          if(!Files.exists(localPomPath)) {
            // If the POM does not exist locally, install it
            PomWriter.write(dependencyPom, localPomPath);
          }
        }

        // Try to install the dependency locally
        if(!target.isInstalled(dependency) && !installArtifact(dependency, target) && dependencyPom == null) {
          // The dependency had no artifact and also no POM
          throw new MavenResolveException("Could not resolve " + dependency);
        }
      }
    }
  }

  /**
   * Determines if the given dependency should be skipped during install.
   *
   * @param dependency The dependency to check
   * @return {@code true} if the dependency should be skipped, {@code false} otherwise
   */
  private boolean shouldSkip(MavenDependency dependency) {
    return dependency.isOptional() ||
        (dependency.getScope() != MavenDependencyScope.COMPILE &&
            dependency.getScope() != MavenDependencyScope.RUNTIME);
  }

  /**
   * Tries to find the given artifact in the sources.
   *
   * @param artifact The artifact to find
   * @return An input stream from which the artifact can be read, or {@code null} if not found in the sources
   * @throws IOException If an I/O error occurs while opening the stream
   */
  public InputStream findArtifactStream(MavenArtifact artifact) throws IOException {
    for(ReadableMavenRepository source : sources) {
      InputStream stream;
      if((stream = source.getArtifactStream(artifact)) != null) {
        // Found the requested artifact
        return stream;
      }
    }

    // Artifact has not been found in any source
    return null;
  }

  /**
   * Tries to find the URI of the given artifact in the sources.
   *
   * @param artifact The artifact to find the URI for
   * @return The found URI, or {@code null}, if not found in the sources
   * @throws IOException If an I/O error occurs while checking for the artifact
   */
  public Pair<ReadableMavenRepository, URI> findArtifactURI(MavenArtifact artifact) throws IOException {
    for(ReadableMavenRepository source : sources) {
      URI uri = source.getArtifactURI(artifact);
      if(uri != null) {
        return new Pair<>(source, uri);
      }
    }

    // Artifact has not been found in any source
    return null;
  }

  /**
   * Tries to find the given POM in the sources.
   *
   * @param artifact The artifact to find the POM for
   * @return The POM for the given artifact, or {@code null} if no matching POM was found in the sources
   * @throws IOException If an I/O error occurs while trying to read a POM
   */
  private MavenPom findPom(MavenArtifact artifact) throws IOException {
    for(ReadableMavenRepository source : sources) {
      MavenPom pom;
      if((pom = source.getArtifactPom(artifact)) != null) {
        // Found the requested POM
        return pom;
      }
    }

    // POM has not been found in any source
    return null;
  }

  /**
   * Installs the given artifact into the given repository.
   *
   * @param artifact The artifact to install
   * @param target   The repository to install the artifact into
   * @return {@code true} if the artifact has been found and installed, {@code false} otherwise
   * @throws IOException If an I/O error occurs while installing the artifact
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean installArtifact(MavenArtifact artifact, SimpleMavenRepository target) throws IOException {
    try(InputStream stream = findArtifactStream(artifact)) {
      // Try to find the given artifact
      if(stream != null) {
        // The artifact has been found, install it
        Path targetPath = target.getArtifactPath(artifact);
        if(!Files.isDirectory(targetPath.getParent())) {
          // Make sure the parent directories exist
          Files.createDirectories(targetPath.getParent());
        }

        LOGGER.lifecycle("Installing artifact {}", formatArtifact(artifact));

        // Copy the artifact to the local path
        Files.copy(stream, targetPath);
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Formats the given artifact as a human readable string.
   *
   * @param artifact The artifact to format
   * @return The formatted artifact
   */
  private String formatArtifact(MavenArtifact artifact) {
    return String.format("%s:%s:%s%s", artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
        (artifact.getClassifier() != null ? ':' + artifact.getClassifier() : "") +
            (artifact.getType() != null ? '@' + artifact.getType() : ""));
  }
}
