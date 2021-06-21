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

package net.flintmc.gradle.java.instrumentation;

import net.flintmc.gradle.java.instrumentation.tasks.InstrumentationTask;
import net.flintmc.gradle.util.Util;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class Instrumentation {

  private static final String GROUP = "flint-instrumentation";

  public void apply(final Project project) {
    // Whether the project has applied the java base plugin
    if (!project.getPlugins().hasPlugin(JavaBasePlugin.class)) {
      return;
    }

    Configuration instrumentation = project.getConfigurations().maybeCreate("instrumentation");
    instrumentation.setCanBeResolved(true);

    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    sourceSets.all(sourceSet -> this.configure(project, sourceSet, instrumentation));
  }

  /**
   * Configures the instrumentation tasks.
   *
   * @param project   The project in which the instrumentation tasks are to be configured.
   * @param sourceSet The {@link SourceSet} in which the instrumentation tasks are to be
   *                  configured.
   */
  private void configure(final Project project, final SourceSet sourceSet, Configuration instrumentationConfiguration) {
    InstrumentationTask instrumentationTask = project.getTasks()
        .create(sourceSet.getTaskName("instrument", "code") + Util.capitalize(project.getName()),
            InstrumentationTask.class);
    instrumentationTask.getOutputs().dir(sourceSet.getOutput().getClassesDirs().getSingleFile().getPath() + "-instrumented");
    instrumentationTask.dependsOn(sourceSet.getClassesTaskName());
    instrumentationTask.setGroup(GROUP);
    instrumentationTask.setSourceSet(sourceSet);
    instrumentationTask.setConfiguration(instrumentationConfiguration);

    Task postInstrumentationTask = project.getTasks()
        .create("post" + Util.capitalize(instrumentationTask.getName()));
    postInstrumentationTask.dependsOn(instrumentationTask);
    postInstrumentationTask.setGroup(GROUP);
    postInstrumentationTask.doLast(task -> {
      ((ConfigurableFileCollection) sourceSet.getOutput().getClassesDirs()).setFrom(instrumentationTask.getOutputs().getFiles().getSingleFile());
    });
    sourceSet.compiledBy(postInstrumentationTask);
  }

}
