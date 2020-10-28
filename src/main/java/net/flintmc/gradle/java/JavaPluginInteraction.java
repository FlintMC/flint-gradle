package net.flintmc.gradle.java;

import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.util.Collection;

public class JavaPluginInteraction {
  private final Project project;

  public JavaPluginInteraction(Project project) {
    this.project = project;
    project.getPluginManager().apply(JavaPlugin.class);
  }

  /**
   * Configures the source sets depending on how the extension has been configured.
   * Versioned setup is handled by the {@link #setupVersioned(FlintGradleExtension, Collection, Collection, String)}
   * method.
   *
   * @param extension The configured extension
   */
  public void setup(FlintGradleExtension extension) {
    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    DependencyHandler dependencies = project.getDependencies();

    if (!extension.shouldDisableInternalSourceSet()) {
      // The internal source set is not disabled, create it and set it
      // as the source set containing the implementation
      SourceSet internalSourceSet = sourceSets.maybeCreate("internal");
      SourceSet mainSourceSet = sourceSets.getByName("main");

      // Add the output of the main source set to the internal source set
      extendSourceSet(internalSourceSet, mainSourceSet);

      // Create the internal configuration
      Configuration internalConfiguration = project.getConfigurations().maybeCreate("internal");
      internalConfiguration.extendsFrom(project.getConfigurations().getByName("implementation"));

      // Add the internal configuration
      dependencies.add("internal", internalSourceSet.getOutput());
      dependencies.add("internal", internalSourceSet.getCompileClasspath());
    }

    for (String configurationName : project.getConfigurations().getAsMap().keySet()) {
      configurationName = Character.toUpperCase(configurationName.charAt(0)) + configurationName.substring(1);

      // Create the syntactic sugar helper to allow invocation of `version<configurationName>("<version>")`
      dependencies.getExtensions().add(
          "versioned" + configurationName,
          new VersionedDependencyAdder(this, configurationName, dependencies)
      );
    }
  }

  /**
   * Configures the source sets depending the dependencies and how the extension has been
   * configured.
   *
   * @param extension           The configured extension
   * @param compileDependencies The versioned compile dependencies
   * @param runtimeDependencies The versioned runtime dependencies
   * @param version             The version to configure
   */
  public void setupVersioned(
      FlintGradleExtension extension,
      Collection<MavenArtifact> compileDependencies,
      Collection<MavenArtifact> runtimeDependencies,
      String version
  ) {
    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

    SourceSet implementationSourceSet = extension.shouldDisableInternalSourceSet() ?
        sourceSets.getByName("main") : sourceSets.getByName("internal");

    // Create the versioned source set by prepending 'v' and replacing all '.' with '_' in the version string
    SourceSet versionedSourceSet = sourceSets.maybeCreate("v" + version.replace('.', '_'));
    versionedSourceSet.getExtensions().add("minecraftVersion", version);

    // Make the implementation source set available to the versioned source set
    extendSourceSet(versionedSourceSet, implementationSourceSet);

    // Retrieve the configuration names
    String compileOnlyConfiguration = versionedSourceSet.getCompileOnlyConfigurationName();
    String runtimeOnlyConfiguration = versionedSourceSet.getRuntimeOnlyConfigurationName();

    for (MavenArtifact compileDependency : compileDependencies) {
      // Add all compile dependencies to compileOnly
      project.getDependencies().add(compileOnlyConfiguration, compileDependency.toIdentifier());
    }

    for (MavenArtifact runtimeDependency : runtimeDependencies) {
      // Add all runtime dependencies to runtimeOnly
      project.getDependencies().add(runtimeOnlyConfiguration, runtimeDependency.toIdentifier());
    }

    for (MavenArtifact runtimeDependency : runtimeDependencies) {
      // Add all runtime dependencies to runtimeOnly
      project.getDependencies().add(runtimeOnlyConfiguration, runtimeDependency.toIdentifier());
    }
  }

  /**
   * Extends the given source set from another one.
   *
   * @param toExtend  The source set to extend
   * @param extension The source set to extend from
   */
  private void extendSourceSet(SourceSet toExtend, SourceSet extension) {
    toExtend.setCompileClasspath(combineFileCollections(
        toExtend.getCompileClasspath(),
        extension.getCompileClasspath(),
        extension.getOutput()
    ));

    toExtend.setRuntimeClasspath(combineFileCollections(
        toExtend.getRuntimeClasspath(),
        extension.getRuntimeClasspath(),
        extension.getOutput()
    ));
  }

  /**
   * Combines the given {@link FileCollection}s into a composite one.
   *
   * @param base        The collection to use as a base
   * @param collections The collections to add
   * @return All collections composited together
   */
  private FileCollection combineFileCollections(FileCollection base, FileCollection... collections) {
    // Initially set the combined one as the base
    FileCollection combined = base;

    for (FileCollection collection : collections) {
      // Chain all of them together, the plus method creates a copy
      combined = combined.plus(collection);
    }

    return combined;
  }
}
