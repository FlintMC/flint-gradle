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

package net.flintmc.gradle.manifest.tasks;

import net.flintmc.gradle.manifest.cache.BoundMavenDependencies;
import net.flintmc.gradle.manifest.data.ManifestMavenDependency;
import net.flintmc.gradle.manifest.data.ManifestMavenDependencyInput;
import net.flintmc.gradle.manifest.data.ManifestRepository;
import net.flintmc.gradle.manifest.data.ManifestRepositoryInput;
import net.flintmc.gradle.maven.RemoteMavenRepository;
import net.flintmc.gradle.maven.cache.MavenArtifactURLCache;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.util.MaybeNull;
import okhttp3.OkHttpClient;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

/**
 * Task resolving the URL's of dependencies for the jar manifest.
 */
public class ResolveArtifactURLsTask extends DefaultTask {
  private final OkHttpClient httpClient;
  private final MavenArtifactURLCache mavenArtifactURLCache;

  @Nested
  private final ManifestRepositoryInput repositoryInput;

  @Nested
  private final ManifestMavenDependencyInput dependencyInput;

  @OutputFile
  private File cacheFile;

  /**
   * Constructs a new {@link ResolveArtifactURLsTask}.
   *
   * @param httpClient            The HTTP client to use for repository access
   * @param mavenArtifactURLCache The cache to use for caching maven artifact URL's
   * @param repositoryInput       The repository inputs to use for resolving
   * @param dependencyInput       The dependency inputs to use for resolving
   */
  @Inject
  public ResolveArtifactURLsTask(
      MaybeNull<OkHttpClient> httpClient,
      MavenArtifactURLCache mavenArtifactURLCache,
      ManifestRepositoryInput repositoryInput,
      ManifestMavenDependencyInput dependencyInput
  ) {
    this.httpClient = httpClient.get();
    this.mavenArtifactURLCache = mavenArtifactURLCache;
    this.repositoryInput = repositoryInput;
    this.dependencyInput = dependencyInput;
  }

  /**
   * Retrieves the repository inputs this task is using.
   *
   * @return The repository inputs this task is using
   */
  @SuppressWarnings("unused") // Required for @Nested on `repositoryInput`
  public ManifestRepositoryInput getRepositoryInput() {
    repositoryInput.compute(getProject());
    return repositoryInput;
  }

  /**
   * Retrieves the dependency inputs this task is using.
   *
   * @return The dependency inputs this task is using
   */
  @SuppressWarnings("unused") // Required for @Nested on `dependencyInput`
  public ManifestMavenDependencyInput getDependencyInput() {
    dependencyInput.compute(getProject());
    return dependencyInput;
  }

  /**
   * Retrieves the cache file this task writes to.
   *
   * @return The cache file this task writes to
   */
  public File getCacheFile() {
    if(cacheFile == null) {
      cacheFile = BoundMavenDependencies.getCacheFile(getProject());
    }

    return cacheFile;
  }

  /**
   * Resolves the repositories and writes them into the cache.
   *
   * @throws IOException If an I/O error occurs
   */
  @TaskAction
  public void resolve() throws IOException {
    File parentDir = getCacheFile().getParentFile();
    if(!parentDir.exists() && !parentDir.mkdirs()) {
      throw new IOException("Failed to create directory " + parentDir.getAbsolutePath());
    }

    repositoryInput.compute(getProject());
    dependencyInput.compute(getProject());

    // Collect all artifacts that need to be resolved
    Set<MavenArtifact> artifactsToResolve = new HashSet<>();
    for(ManifestMavenDependency dependency : dependencyInput.getDependencies()) {
      artifactsToResolve.add(dependency.getArtifact());
    }

    // Collect all repositories
    Set<RemoteMavenRepository> repositories = new HashSet<>();
    for(ManifestRepository repository : repositoryInput.getRepositories()) {
      if(repository.requiresAuthentication()) {
        // Add repository with authentication
        repositories.add(new RemoteMavenRepository(
            httpClient,
            repository.getURI(),
            repository.getCredentialHeader(),
            repository.getCredentialContent()
        ));
      } else {
        // Add repository without authentication
        repositories.add(new RemoteMavenRepository(
            httpClient,
            repository.getURI()
        ));
      }
    }

    // Resolve all artifact URI's
    Map<MavenArtifact, URI> resolvedArtifacts = mavenArtifactURLCache.resolve(
        artifactsToResolve,
        repositories,
        false
    );

    // Map back the artifacts to dependencies
    Map<ManifestMavenDependency, URI> dependencyURIs = new HashMap<>();
    for(Map.Entry<MavenArtifact, URI> resolved : resolvedArtifacts.entrySet()) {
      dependencyURIs.put(new ManifestMavenDependency(resolved.getKey()), resolved.getValue());
    }

    // Write the resolved data to the cache
    BoundMavenDependencies.save(cacheFile, dependencyURIs);
  }
}
