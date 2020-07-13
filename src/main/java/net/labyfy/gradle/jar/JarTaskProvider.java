package net.labyfy.gradle.jar;

import net.labyfy.gradle.LabyfyGradleException;
import net.labyfy.gradle.LabyfyGradlePlugin;
import net.labyfy.gradle.extension.LabyfyGradleExtension;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.stream.Collectors;

public class JarTaskProvider {


  private final LabyfyGradlePlugin labyfyGradlePlugin;

  public JarTaskProvider(LabyfyGradlePlugin labyfyGradlePlugin) {
    this.labyfyGradlePlugin = labyfyGradlePlugin;
  }

  /**
   * Creates jar and publish tasks for the minecraft sourcesets.
   *
   * @param project   the project to install the tasks in
   * @param extension labyfy gradle extension to fetch data from
   */
  public void installTasks(Project project, LabyfyGradleExtension extension) {
    project.getConfigurations().maybeCreate("labyfy");
    JavaPluginConvention plugin = project.getConvention().getPlugin(JavaPluginConvention.class);
    Set<String> minecraftVersions = extension.getMinecraftVersions()
        .stream()
        .map(name -> name.matches("[0-9]+(.[0-9]+)+") ? ("v" + name.replace('.', '_')) : name)
        .collect(Collectors.toSet());
    minecraftVersions.add("main");
    minecraftVersions.add("internal");
    plugin.getSourceSets()
        .stream()
        .filter(sourceSet -> minecraftVersions.contains(sourceSet.getName()))
        .forEach(sourceSet -> {
          this.createJarTask(sourceSet, project);
          if (!sourceSet.getName().equals("main")) {
            this.installCompileTask(sourceSet, project);
          }
        });
    this.createPublishTask(project);
  }

  /**
   * Creates jar tasks for the minecraft sourcesets.
   *
   * @param project   the project to install the tasks in
   * @param extension labyfy gradle extension to fetch data from
   */
  public void createJarTask(SourceSet sourceSet, Project project) {
    project.getTasks().create(sourceSet.getName() + "Jar", Jar.class, jarTask -> {
      jarTask.setGroup("build");
      jarTask.getArchiveAppendix().set(sourceSet.getName());
      jarTask.doFirst(task -> {
        jarTask.from(sourceSet.getAllJava().getAsFileTree());
        ((Jar) project.getTasks().getByName("jar")).from(project.zipTree(jarTask.getOutputs().getFiles().getSingleFile()));
      });
      project.getTasks().getByName("jar").dependsOn(jarTask.getName());
    });
  }

  /**
   * Creates publish tasks for the minecraft sourcesets.
   *
   * @param project the project to install the tasks in
   */
  public void createPublishTask(Project project) {
    project.getTasks().create("labyfyArtifactPublish", jarTask -> {
      jarTask.setGroup("publish");
      jarTask.dependsOn("jar");
      jarTask.doLast(task -> {
        Jar buildTask = (Jar) project.getTasks().getByName("jar");
        File outputFile = buildTask.getOutputs().getFiles().getSingleFile();
        try {
          if (outputFile.exists())
            this.labyfyGradlePlugin.getAssetPublisher().publish(outputFile, project.getName(), project.getVersion().toString(), labyfyGradlePlugin.getExtension().getPublishToken());
        } catch (FileNotFoundException e) {
          throw new LabyfyGradleException("file " + outputFile.getName() + " is not found.", e);
        }
      });
    });
  }

  /**
   * Installs gradle task to compile a given sourceSet in a given project.
   *
   * @param sourceSet The sourceSet to obtain classpath from for compilation
   * @param project   The project to install task on
   */
  public void installCompileTask(SourceSet sourceSet, Project project) {
    project.getTasks().getByName("compileJava").finalizedBy(sourceSet.getCompileJavaTaskName());
  }

}
