package net.flintmc.gradle.java.instrumentation.tasks;

import java.io.File;
import javax.inject.Inject;
import net.flintmc.gradle.java.instrumentation.InstrumentationTransformerRegistry;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

public class InstrumentationTask extends DefaultTask {

  private final InstrumentationTransformerRegistry registry;

  @Internal
  private SourceSet sourceSet;

  @Inject
  public InstrumentationTask(final InstrumentationTransformerRegistry registry) {
    this.registry = registry;
  }

  public SourceSet getSourceSet() {
    return sourceSet;
  }

  public void setSourceSet(final SourceSet sourceSet) {
    this.sourceSet = sourceSet;
  }

  @TaskAction
  public void executeInstrumentation() {
    this.registry.getTransformers().forEach(
        transformer -> transformer.transform(this.sourceSet, this.getOriginalClassesDirectories()));
  }

  private FileCollection getOriginalClassesDirectories() {
    return this.sourceSet.getOutput().getClassesDirs();
  }

  private File getOriginalResourcesDirectory() {
    return this.sourceSet.getOutput().getResourcesDir();
  }

}
