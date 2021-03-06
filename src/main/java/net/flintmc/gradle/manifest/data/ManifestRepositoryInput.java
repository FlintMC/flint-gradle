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

import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.credentials.HttpHeaderCredentials;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.authentication.Authentication;
import org.gradle.authentication.http.HttpHeaderAuthentication;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Cacheable listing of manifest repositories.
 */
public class ManifestRepositoryInput {
  private final Set<ManifestRepository> repositories;
  private final List<String> warnings;

  private boolean computed;

  public ManifestRepositoryInput() {
    repositories = new HashSet<>();
    warnings = new ArrayList<>();
  }

  /**
   * Computes the input for the given project.
   *
   * @param project The project to compute the input for
   */
  public void compute(Project project) {
    if(computed) {
      return;
    }

    for(ArtifactRepository repository : project.getRepositories()) {
      if(repository instanceof MavenArtifactRepository) {
        // Found a maven repository, the only type we currently process
        MavenArtifactRepository mavenArtifactRepository = (MavenArtifactRepository) repository;
        URI repositoryURI = mavenArtifactRepository.getUrl();

        if(!repositoryURI.getScheme().equals("http") && !repositoryURI.getScheme().equals("https")) {
          continue;
        }

        String authenticationHeader = null;
        String authenticationContent = null;

        if(!mavenArtifactRepository.getAuthentication().withType(HttpHeaderAuthentication.class).isEmpty()) {
          // The repository uses some header authentication
          HttpHeaderCredentials credentials = mavenArtifactRepository.getCredentials(HttpHeaderCredentials.class);

          authenticationHeader = credentials.getName();
          authenticationContent = credentials.getValue();
        } else if(!mavenArtifactRepository.getAuthentication().isEmpty()) {
          for(Authentication authentication : mavenArtifactRepository.getAuthentication()) {
            warnings.add(String.format(
                "Maven repository %s uses unsupported authentication of type %s, " +
                    "it might not be included in manifest dependency resolution",
                repository.getName(),
                authentication.getName()
            ));
          }
        }

        // Add the found repository
        repositories.add(
            new ManifestRepository(mavenArtifactRepository.getUrl(), authenticationHeader, authenticationContent));
      } else {
        warnings.add(String.format(
            "Repository %s is of unsupported type %s, it will not be included in manifest dependency resolution",
            repository.getName(),
            repository.getClass().getName()
        ));
      }
    }

    computed = true;
  }

  /**
   * Retrieves a list of warnings that may have occurred while computing the repositories.
   *
   * @return A list of warnings
   */
  @Internal
  public List<String> getWarnings() {
    if(!computed) {
      throw new IllegalStateException("Input has not been computed yet");
    }

    return warnings;
  }

  /**
   * Retrieves all remote repositories that have been defined.
   *
   * @return All remote repositories
   */
  @Input
  public Set<ManifestRepository> getRepositories() {
    if(!computed) {
      throw new IllegalStateException("Input has not been computed yet");
    }

    return repositories;
  }
}
