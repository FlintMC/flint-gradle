package net.flintmc.gradle.minecraft;

import net.flintmc.gradle.util.Reference;
import net.flintmc.gradle.util.Util;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the potential classpath minecraft might have.
 */
public class PotentialMinecraftClasspath {
  private final Map<SourceSet, Project> sourceSets;

  /**
   * Constructs a new empty potential classpath.
   */
  public PotentialMinecraftClasspath() {
    this.sourceSets = new HashMap<>();
  }

  /**
   * Puts the given source set belonging to the given project on the potential classpath.
   *
   * @param sourceSet The source set to put on the classpath
   * @param project   The project the source set belongs to
   */
  public void put(SourceSet sourceSet, Project project) {
    this.sourceSets.put(sourceSet, project);
  }

  /**
   * Puts the given source sets belonging to the given projects on the potential classpath.
   *
   * @param sourceSets The source sets and their respective projects to put on the potential classpath
   */
  public void putAll(Map<SourceSet, Project> sourceSets) {
    this.sourceSets.putAll(sourceSets);
  }

  /**
   * Retrieves the tasks required to build the input for this classpath.
   *
   * @param minecraftVersion The minecraft version to retrieve the tasks for
   * @return The tasks required to build the input for this classpath
   */
  public Set<Task> getTaskDependencies(String minecraftVersion) {
    Set<Task> tasks = new HashSet<>();
    sourceSets.forEach((sourceSet, project) -> {
      if(isSourceSetUsableWith(sourceSet, minecraftVersion)) {
        // The source set can be used with the minecraft version, retrieve the task
        // building the source set and add it to the dependency list
        Task task = project.getTasks().findByName(sourceSet.getJarTaskName());
        if(task == null) {
          // Fall back to the Java task name if the source set does not generate a jar
          task = project.getTasks().findByName(sourceSet.getCompileJavaTaskName());
        }

        if(task != null) {
          tasks.add(task);
        }
      }
    });
    return tasks;
  }

  /**
   * Retrieves the real runtime classpath for the given minecraft version.
   *
   * @param project          The project to retrieve the classpath for
   * @param minecraftVersion The minecraft version the classpath required
   * @return The classpath for the given minecraft version
   */
  public FileCollection getRealClasspath(Project project, String minecraftVersion) {
    // Use Reference so the variable can be altered from within the lambda
    Reference<FileCollection> fileCollection = new Reference<>(project.files());

    sourceSets.forEach((sourceSet, sourceProject) -> {
      if(isSourceSetUsableWith(sourceSet, minecraftVersion)) {
        fileCollection.set(
            Util.deduplicatedFileCollection(project, fileCollection.get(), sourceSet.getRuntimeClasspath()));
      }
    });

    return fileCollection.get();
  }

  /**
   * Tests if the given source set is usable with the given minecraft version-
   *
   * @param sourceSet        The source set to test
   * @param minecraftVersion The minecraft version to test for
   * @return {@code true} if the source set can be used with the given version, {@code false} otherwise
   */
  private boolean isSourceSetUsableWith(SourceSet sourceSet, String minecraftVersion) {
    Object minecraftVersionExtension = sourceSet.getExtensions().findByName("minecraftVersion");

    return minecraftVersionExtension == null || minecraftVersionExtension.toString().equals(minecraftVersion);
  }
}
