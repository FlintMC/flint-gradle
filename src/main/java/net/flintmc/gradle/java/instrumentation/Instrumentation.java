package net.flintmc.gradle.java.instrumentation;

import net.flintmc.gradle.java.instrumentation.tasks.InstrumentationTask;
import net.flintmc.gradle.util.Util;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class Instrumentation {

  private static final String GROUP = "flint-instrumentation";
  private InstrumentationTransformerRegistry instrumentationRegistry;

  public void apply(final Project project,
      final InstrumentationTransformerRegistry instrumentationRegistry) {
    // Whether the project has applied the java base plugin
    if (!project.getPlugins().hasPlugin(JavaBasePlugin.class)) {
      return;
    }

    this.instrumentationRegistry = instrumentationRegistry;

    SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
    sourceSets.all(sourceSet -> this.configure(project, sourceSet));
  }

  /**
   * Configures the instrumentation tasks.
   *
   * @param project   The project in which the instrumentation tasks are to be configured.
   * @param sourceSet The {@link SourceSet} in which the instrumentation tasks are to be
   *                  configured.
   */
  private void configure(final Project project, final SourceSet sourceSet) {
    InstrumentationTask instrumentationTask = project.getTasks()
        .create(sourceSet.getTaskName("instrument", "code") + Util.capitalize(project.getName()),
            InstrumentationTask.class, this.instrumentationRegistry);
    instrumentationTask.dependsOn(sourceSet.getClassesTaskName());
    instrumentationTask.setGroup(GROUP);
    instrumentationTask.setSourceSet(sourceSet);

    Task postInstrumentationTask = project.getTasks()
        .create("post" + Util.capitalize(instrumentationTask.getName()));
    postInstrumentationTask.dependsOn(instrumentationTask);
    postInstrumentationTask.setGroup(GROUP);

    sourceSet.compiledBy(postInstrumentationTask);
  }

}
