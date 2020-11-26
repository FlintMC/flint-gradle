package net.flintmc.gradle.manifest;

import net.flintmc.gradle.extension.FlintGradleExtension;
import org.gradle.api.Project;
import org.gradle.api.component.SoftwareComponent;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;

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
      PublishingExtension publishingExtension = project.getExtensions().findByType(PublishingExtension.class);
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
