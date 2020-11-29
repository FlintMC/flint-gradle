package net.flintmc.gradle.manifest;

import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.manifest.data.ManifestMavenDependencyInput;
import net.flintmc.gradle.manifest.data.ManifestRepositoryInput;
import net.flintmc.gradle.manifest.data.ManifestStaticFileInput;
import net.flintmc.gradle.maven.cache.MavenArtifactURLCache;
import net.flintmc.gradle.property.FlintPluginProperties;
import net.flintmc.gradle.util.MaybeNull;
import net.flintmc.gradle.util.Util;
import org.apache.http.client.HttpClient;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.credentials.HttpHeaderCredentials;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.authentication.http.HttpHeaderAuthentication;

import java.net.URI;

public class ManifestConfigurator {
  private final Project project;
  private final HttpClient httpClient;
  private final MavenArtifactURLCache mavenArtifactURLCache;

  /**
   * Constructs a new {@link ManifestConfigurator} for the given plugin.
   *
   * @param plugin The plugin to configure the manifest for
   */
  public ManifestConfigurator(FlintGradlePlugin plugin) {
    this.project = plugin.getProject();
    this.httpClient = plugin.getHttpClient();
    this.mavenArtifactURLCache = plugin.getMavenArtifactURLCache();
  }

  private URI distributorMavenURI;
  private URI projectMavenURI;

  /**
   * Installs the required gradle tasks to generate the flint manifests.
   */
  public void configure() {
    if(!isValidProject(project)) {
      return;
    }

    FlintGradleExtension extension = project.getExtensions().getByType(FlintGradleExtension.class);

    if(extension.shouldAutoConfigurePublishing() && extension.shouldEnablePublishing()) {
      // Auto configuration is enabled
      PublishingExtension publishingExtension = project.getExtensions().findByType(PublishingExtension.class);

      // Build the distributor URL in form of <host>/maven/<channel>
      URI distributorUrl = getDistributorMavenURI(
          "Set enablePublishing to false in the flint extension",
          "Set shouldAutoConfigurePublishing to false in the flint extension");

      // Retrieve either a bearer or publish token
      String bearerToken = FlintPluginProperties.DISTRIBUTOR_BEARER_TOKEN.resolve(project);
      String publishToken = null;
      if(bearerToken == null) {
        publishToken = FlintPluginProperties.DISTRIBUTOR_PUBLISH_TOKEN
            .require(project, "Set enablePublishing to false in the flint extension",
                "Set shouldAutoConfigurePublishing to false in the flint extension");
      }

      if(publishingExtension != null) {
        // Found a publishing extension, automatically set the publish target
        MavenPublication publication =
            publishingExtension.getPublications().create("flint", MavenPublication.class);

        // Configure the publication
        publication.setGroupId(project.getGroup().toString());
        publication.setArtifactId(project.getName());
        publication.setVersion(project.getVersion().toString());

        // Add all components to the publication
        for(SoftwareComponent component : project.getComponents()) {
          publication.from(component);
        }

        // Configure the repository
        MavenArtifactRepository repository =
            publishingExtension.getRepositories().maven((repo) -> repo.setName("Flint Distributor"));
        repository.setUrl(distributorUrl);
        HttpHeaderCredentials credentials = repository.getCredentials(HttpHeaderCredentials.class);

        if(bearerToken != null) {
          // User has selected bearer authorization
          credentials.setName("Authorization");
          credentials.setValue("Bearer " + bearerToken);
        } else {
          // User has selected publish token authorization
          credentials.setName("Publish-Token");
          credentials.setValue(publishToken);
        }

        // Set the authentication, no further configuration required
        repository.getAuthentication().create("header", HttpHeaderAuthentication.class);
      }
    }

    // Create the task inputs
    ManifestMavenDependencyInput mavenDependencyInput = new ManifestMavenDependencyInput();
    ManifestRepositoryInput repositoryInput = new ManifestRepositoryInput();
    ManifestStaticFileInput staticFileInput = new ManifestStaticFileInput();

    // Compute the inputs
    mavenDependencyInput.compute(project);
    repositoryInput.compute(project);
    staticFileInput.compute(project, this);

    // Create the tasks
    project.getTasks().create(
        "resolveArtifactURLs",
        ResolveArtifactURLsTask.class,
        new MaybeNull<>(httpClient),
        mavenArtifactURLCache,
        repositoryInput,
        mavenDependencyInput
    );
  }

  /**
   * Determines whether the given project is valid flint project.
   *
   * @param project The project to test
   * @return {@code true} if the project is a valid flint project, {@code false} otherwise
   */
  private boolean isValidProject(Project project) {
    FlintGradleExtension remote = project.getExtensions().findByType(FlintGradleExtension.class);
    return remote != null && remote.getProjectFilter().test(project);
  }

  /**
   * Retrieves the base URI of the distributor repository.
   *
   * @param notAvailableSolution Messages to display as a solution in case URI can't be computed
   * @return The base URI of the distributor repository
   */
  public URI getDistributorMavenURI(String... notAvailableSolution) {
    if(distributorMavenURI == null) {
      distributorMavenURI = Util.concatURI(
          FlintPluginProperties.DISTRIBUTOR_URL
              .require(project, notAvailableSolution),
          "maven",
          FlintPluginProperties.DISTRIBUTOR_CHANNEL
              .require(project, notAvailableSolution)
      );
    }

    return distributorMavenURI;
  }

  /**
   * Retrieves the base URI of the distributor repository including the project namespace.
   *
   * @param notAvailableSolution Messages to display as a solution in case the URI can't be computed
   * @return The base URI of the distributor repository including the project namespace
   */
  public URI getProjectMavenURI(String... notAvailableSolution) {
    if(projectMavenURI == null) {
      URI distributorURI = getDistributorMavenURI(notAvailableSolution);

      projectMavenURI = Util.concatURI(
          distributorURI,
          project.getGroup().toString().replace('.', '/'),
          project.getName(),
          project.getVersion().toString()
      );
    }

    return projectMavenURI;
  }
}
