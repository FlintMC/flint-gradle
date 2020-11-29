package net.flintmc.gradle.manifest;

import net.flintmc.gradle.manifest.data.ManifestMavenDependency;
import net.flintmc.gradle.manifest.data.ManifestMavenDependencyInput;
import net.flintmc.gradle.manifest.data.ManifestRepository;
import net.flintmc.gradle.manifest.data.ManifestRepositoryInput;
import net.flintmc.gradle.maven.RemoteMavenRepository;
import net.flintmc.gradle.maven.cache.MavenArtifactURLCache;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.util.MaybeNull;
import org.apache.http.client.HttpClient;
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
  private final HttpClient httpClient;
  private final MavenArtifactURLCache mavenArtifactURLCache;

  @Nested
  private final ManifestRepositoryInput repositoryInput;

  @Nested
  private final ManifestMavenDependencyInput dependencyInput;

  @OutputFile
  private File cacheFile;

  /**
   * Creates a new {@link ResolveArtifactURLsTask}.
   *
   * @param httpClient            The HTTP client to use for repository access
   * @param mavenArtifactURLCache The cache to use for caching maven artifact URL's
   * @param repositoryInput       The repository inputs to use for resolving
   * @param dependencyInput       The dependency inputs to use for resolving
   */
  @Inject
  public ResolveArtifactURLsTask(
      MaybeNull<HttpClient> httpClient,
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
  public ManifestRepositoryInput getRepositoryInput() {
    return repositoryInput;
  }

  /**
   * Retrieves the dependency inputs this task is using.
   *
   * @return The dependency inputs this task is using
   */
  public ManifestMavenDependencyInput getDependencyInput() {
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
        repositories
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
