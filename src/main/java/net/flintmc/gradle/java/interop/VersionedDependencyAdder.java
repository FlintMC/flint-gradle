package net.flintmc.gradle.java.interop;

import net.flintmc.gradle.extension.FlintGradleExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Utility class for adding dependencies using a versioned name
 */
public class VersionedDependencyAdder implements BiConsumer<String, Action<DependencyHandler>> {
  private final Project project;
  private final Map<String, DependencyHandler> proxiedDependencyHandlers;

  /**
   * Constructs a new {@link VersionedDependencyAdder} and initializes it with an empty map of proxied handlers.
   *
   * @param project The project to create the dependency adder for
   */
  public VersionedDependencyAdder(Project project) {
    this.project = project;
    this.proxiedDependencyHandlers = new HashMap<>();
  }

  @Override
  public void accept(String version, Action<DependencyHandler> dependencyHandlerAction) {
    // The extension needs be configured by now
    FlintGradleExtension flintGradleExtension = project.getExtensions().getByType(FlintGradleExtension.class);
    flintGradleExtension.ensureConfigured();

    if(!flintGradleExtension.getMinecraftVersions().contains(version)) {
      throw new IllegalArgumentException("Can't define dependencies for a minecraft version not part of the project");
    }

    // Retrieve or create a dependency handler proxy
    DependencyHandler handler = proxiedDependencyHandlers.computeIfAbsent(version, this::createProxy);
    dependencyHandlerAction.execute(handler);
  }

  /**
   * Creates a new dependency handler proxy for the given minecraft version.
   *
   * @param version The minecraft version to create the proxy for
   * @return The created proxy
   */
  private DependencyHandler createProxy(String version) {
    return DependencyHandlerProxy.of(project.getDependencies(), project.getConfigurations(), version);
  }
}
