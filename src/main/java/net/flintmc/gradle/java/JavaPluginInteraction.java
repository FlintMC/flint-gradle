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

package net.flintmc.gradle.java;

import net.flintmc.gradle.java.interop.FlintDependencyAdder;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.support.GroovyDependencyHandlerExtensions;
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
   * Sets up the plugin interaction including the internal source set and dependency handler extensions.
   * {@link #setupVersioned(Collection, Collection, String)} method.
   */
  public void setup() {
    FlintDependencyAdder dependencyAdder = new FlintDependencyAdder(project);

    // Add the extension for use by DSL specific extensions
    project.getDependencies().getExtensions().add(
        "flintDependencyAdder",
        dependencyAdder
    );

    // Groovy build scripts need manual installation of the dependency handler extensions
    GroovyDependencyHandlerExtensions.install(project);

    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    DependencyHandler dependencies = project.getDependencies();

    // Create the internal source set
    SourceSet internalSourceSet = sourceSets.maybeCreate("internal");
    SourceSet mainSourceSet = sourceSets.getByName("main");

    // Add the output of the main source set to the internal source set
    extendSourceSet(internalSourceSet, mainSourceSet);

    Configuration runtimeConfiguration = project.getConfigurations().getByName("runtimeClasspath");

    // Create the internal configuration
    Configuration internalConfiguration = project.getConfigurations().maybeCreate("internal");
    internalConfiguration.extendsFrom(project.getConfigurations().getByName("implementation"));

    runtimeConfiguration.extendsFrom(internalConfiguration);

    // Add the internal configuration
    dependencies.add("internal", internalSourceSet.getOutput());
    dependencies.add("internal", internalSourceSet.getCompileClasspath());
  }

  /**
   * Configures the source sets depending the dependencies and how the extension has been configured.
   *
   * @param compileDependencies The versioned compile dependencies
   * @param runtimeDependencies The versioned runtime dependencies
   * @param version             The version to configure
   */
  public void setupVersioned(
      Collection<MavenArtifact> compileDependencies,
      Collection<MavenArtifact> runtimeDependencies,
      String version
  ) {
    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    SourceSet implementationSourceSet = sourceSets.getByName("internal");

    // Create the versioned source set by prepending 'v' and replacing all '.' with '_' in the version string
    SourceSet versionedSourceSet = sourceSets.maybeCreate("v" + version.replace('.', '_'));
    versionedSourceSet.getExtensions().add("minecraftVersion", version);

    // Make the implementation source set available to the versioned source set
    extendSourceSet(versionedSourceSet, implementationSourceSet);

    // Retrieve the configuration names
    String compileOnlyConfiguration = versionedSourceSet.getCompileOnlyConfigurationName();
    String runtimeOnlyConfiguration = versionedSourceSet.getRuntimeOnlyConfigurationName();

    for(MavenArtifact compileDependency : compileDependencies) {
      // Add all compile dependencies to compileOnly
      project.getDependencies().add(compileOnlyConfiguration, compileDependency.toIdentifier());
    }

    for(MavenArtifact runtimeDependency : runtimeDependencies) {
      // Add all runtime dependencies to runtimeOnly
      project.getDependencies().add(runtimeOnlyConfiguration, runtimeDependency.toIdentifier());
    }

    for(MavenArtifact runtimeDependency : runtimeDependencies) {
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

    for(FileCollection collection : collections) {
      // Chain all of them together, the plus method creates a copy
      combined = combined.plus(collection);
    }

    return combined;
  }
}
