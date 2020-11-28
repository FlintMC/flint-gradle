package net.flintmc.gradle.extension;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.util.Configurable;

import javax.annotation.Nonnull;

/**
 * Static files block in the flint extension.
 */
public class FlintStaticFilesExtension implements Configurable<NamedDomainObjectContainer<FlintStaticFileDescription>> {
  private final NamedDomainObjectContainer<FlintStaticFileDescription> staticFileDescriptions;

  /**
   * Constructs a new {@link FlintStaticFilesExtension} for the given project.
   *
   * @param project The project owning this extension
   */
  public FlintStaticFilesExtension(Project project) {
    this.staticFileDescriptions = project.container(
        FlintStaticFileDescription.class, (name) -> new FlintStaticFileDescription(project, name));
  }

  /**
   * Retrieves the container of static file descriptions.
   *
   * @return The container of static file descriptions
   */
  public NamedDomainObjectContainer<FlintStaticFileDescription> getStaticFileDescriptions() {
    return staticFileDescriptions;
  }

  @Override
  @Nonnull
  public NamedDomainObjectContainer<FlintStaticFileDescription> configure(@Nonnull Closure cl) {
    return staticFileDescriptions.configure(cl);
  }
}
