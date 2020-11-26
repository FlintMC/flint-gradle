package net.flintmc.gradle.manifest;

import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.property.FlintPluginProperties;
import net.flintmc.gradle.util.Util;
import org.gradle.api.Project;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.credentials.HttpHeaderCredentials;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.authentication.http.HttpHeaderAuthentication;

import java.net.URI;

public class ManifestConfigurator {
  /**
   * Installs the required gradle tasks to generate the flint manifests.
   *
   * @param project The project to install the tasks on
   */
  public void configureProject(Project project) {
    if(!isValidProject(project)) {
      return;
    }

    FlintGradleExtension extension = project.getExtensions().getByType(FlintGradleExtension.class);
    if(extension.shouldAutoConfigurePublishing()) {
      // Auto configuration is enabled
      PublishingExtension publishingExtension = project.getExtensions().findByType(PublishingExtension.class);

      // Build the distributor URL in form of <host>/maven/<channel>
      URI distributorUrl = Util.concatURI(
          FlintPluginProperties.DISTRIBUTOR_URL
              .require(project, "Set shouldAutoConfigurePublishing to false in the flint extension"),
          "maven",
          FlintPluginProperties.DISTRIBUTOR_CHANNEL
              .require(project, "Set shouldAutoConfigurePublishing to false in the flint extension")
      );

      // Retrieve either a bearer or publish token
      String bearerToken = FlintPluginProperties.DISTRIBUTOR_BEARER_TOKEN.resolve(project);
      String publishToken = null;
      if(bearerToken == null) {
        publishToken = FlintPluginProperties.DISTRIBUTOR_PUBLISH_TOKEN
            .require(project, "Set shouldAutoConfigurePublishing to false in the flint extension");
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
}
