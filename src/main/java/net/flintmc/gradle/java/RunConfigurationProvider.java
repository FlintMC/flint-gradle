package net.flintmc.gradle.java;

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.extension.FlintRunsExtension;
import net.flintmc.gradle.minecraft.*;
import net.flintmc.gradle.minecraft.data.version.VersionManifest;
import net.flintmc.gradle.minecraft.yggdrasil.YggdrasilAuthenticator;
import net.flintmc.gradle.util.MaybeNull;
import net.flintmc.gradle.util.Pair;
import okhttp3.OkHttpClient;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for adding run configurations to the root project.
 */
public class RunConfigurationProvider {
  private final Project project;
  private final MinecraftRepository minecraftRepository;
  private final Path runCacheDir;
  private final YggdrasilAuthenticator authenticator;
  private final OkHttpClient httpClient;

  private final Map<String, Pair<MinecraftRunTask, InstallStaticFilesTask>> tasks;
  private final Map<String, PotentialMinecraftClasspath> configurationClasspaths;

  /**
   * Constructs a new {@link RunConfigurationProvider} for the given project.
   *
   * @param project             The project to generate configurations on
   * @param minecraftRepository The repository to retrieve minecraft launch information from
   * @param runCacheDir         The directory certain thins such as natives and assets are cached in
   * @param authenticator       The {@link YggdrasilAuthenticator} passed to minecraft run tasks for authentication
   * @param httpClient          The HTTP client to use for potential downloads
   */
  public RunConfigurationProvider(
      Project project,
      MinecraftRepository minecraftRepository,
      Path runCacheDir,
      YggdrasilAuthenticator authenticator,
      OkHttpClient httpClient
  ) {
    this.project = project;
    this.minecraftRepository = minecraftRepository;
    this.runCacheDir = runCacheDir;
    this.authenticator = authenticator;
    this.httpClient = httpClient;

    this.tasks = new HashMap<>();
    this.configurationClasspaths = new HashMap<>();
  }

  /**
   * Installs the given source sets into the configurations
   *
   * @param sourceProject The project to use as a source for the source sets
   * @param extension     The extension to use as a configuration base
   */
  public void installSourceSets(Project sourceProject, FlintGradleExtension extension) {
    FlintRunsExtension runs = extension.getRuns();
    SourceSetContainer sourceSets = sourceProject.getExtensions().getByType(SourceSetContainer.class);

    // Retrieve the configurations for which all source sets should be included
    // (unless later explicitly excluded)
    Set<String> allIncludedConfigurations = runs.getAllIncludedConfigurations();

    // Get the special configurations and resolve their source set names
    Map<String, Map<SourceSet, Project>> includedSourceSetsByConfiguration =
        resolveNames(runs.getIncludedSourceSets(), sourceSets, sourceProject);
    Map<String, Map<SourceSet, Project>> excludedSourceSetsByConfiguration =
        resolveNames(runs.getExcludedSourceSets(), sourceSets, sourceProject);

    for (String configuration : allIncludedConfigurations) {
      for (SourceSet sourceSet : sourceSets) {
        // Test if the source set has not been explicitly excluded for the given configuration
        if (!excludedSourceSetsByConfiguration.containsKey(configuration) ||
            !excludedSourceSetsByConfiguration.get(configuration).containsKey(sourceSet) ||
            sourceSet.getName().equals("test") // Exclude the test source set, it can be manually included if wanted
        ) {
          // The source set has not been excluded, add it
          PotentialMinecraftClasspath configurationSourceSets =
              configurationClasspaths.computeIfAbsent(configuration, k -> new PotentialMinecraftClasspath());

          ensureVersionedTasksSetup(configuration, extension.getMinecraftVersions(), configurationSourceSets);
          configurationSourceSets.put(sourceSet, sourceProject);
        }
      }
    }

    // Iterate all explicitly included source sets
    includedSourceSetsByConfiguration.forEach((configuration, included) -> {
      // Add the source sets
      PotentialMinecraftClasspath configurationSourceSets = configurationClasspaths.computeIfAbsent(
          configuration, k -> new PotentialMinecraftClasspath());
      ensureVersionedTasksSetup(configuration, extension.getMinecraftVersions(), configurationSourceSets);
      configurationSourceSets.putAll(included);
    });
  }

  /**
   * Ensures that all tasks are set up for the given versions and configuration name.
   *
   * @param configuration      The name of the configuration
   * @param versions           The versions to set runs up for
   * @param potentialClasspath The classpath to set the tasks up for
   */
  private void ensureVersionedTasksSetup(String configuration, Set<String> versions,
                                         PotentialMinecraftClasspath potentialClasspath) {
    for (String version : versions) {
      ensureRunTaskSetup(configuration, version, potentialClasspath);
    }
  }

  /**
   * Ensures that a run task for the given configuration and version exists.
   *
   * @param configuration      The configuration run name
   * @param version            The minecraft version of the run task
   * @param potentialClasspath The potential classpath for the task
   */
  private void ensureRunTaskSetup(String configuration, String version, PotentialMinecraftClasspath potentialClasspath) {
    // Generate a task name
    String runTaskName = "runClient" + version + getConfigurationName(configuration);
    if (!tasks.containsKey(runTaskName)) {
      // The task does not yet exist, create it
      MinecraftRunTask runTask = createRunTask(runTaskName, version);
      runTask.setConfigurationName(configuration);

      // Retrieve the runs extension of the root project
      FlintRunsExtension extension = project.getExtensions().getByType(FlintGradleExtension.class).getRuns();
      Map<String, String> overrides = extension.getMainClassOverrides();

      // Retrieve a matching override or null
      String mainClassOverride = overrides.containsKey(configuration) ?
          overrides.get(configuration) : extension.getGeneralMainClassOverride();

      if (mainClassOverride != null) {
        // Override has been set, pass it to the task
        runTask.setMain(mainClassOverride);
      }

      // Set up the authenticator
      runTask.setAuthenticator(authenticator);

      // Set the potential classpath and index the task
      runTask.setPotentialClasspath(potentialClasspath);

      // Create the static files task
      InstallStaticFilesTask installStaticFiles = project.getTasks().create(
          "installStaticFilesFor" + version + getConfigurationName(configuration),
          InstallStaticFilesTask.class,
          new MaybeNull<>(httpClient),
          potentialClasspath,
          version,
          runTask.getWorkingDir()
      );

      // Set up the dependencies of the tasks
      Set<Task> taskDependencies = potentialClasspath.getTaskDependencies(version);
      installStaticFiles.dependsOn(taskDependencies);

      runTask.dependsOn(installStaticFiles);

      tasks.put(runTaskName, new Pair<>(runTask, installStaticFiles));
    } else {
      // The task exists already
      Pair<MinecraftRunTask, InstallStaticFilesTask> tasks = this.tasks.get(runTaskName);
      tasks.getSecond().dependsOn(potentialClasspath.getTaskDependencies(version));

      if (tasks.getFirst().getPotentialClasspath() != potentialClasspath) {
        // This means something tried to replace the set of source sets afterwards, this would break
        // the discovery mechanism
        throw new IllegalArgumentException("Can't redefine the potential classpath of a minecraft run task");
      }
    }
  }

  /**
   * Creates the minecraft run task with the given name for the given version.
   *
   * @param name    The name of the task
   * @param version The minecraft version to create the task for
   * @return The created task
   */
  private MinecraftRunTask createRunTask(String name, String version) {
    // Create the task itself
    MinecraftRunTask task = project.getTasks().create(name, MinecraftRunTask.class);

    // Build a path for the run directory
    Path runDir = project.file("run/" + version).toPath();
    if (!Files.isDirectory(runDir)) {
      // The run directory does not exist, try to create it
      try {
        Files.createDirectories(runDir);
      } catch (IOException e) {
        throw new FlintGradleException("Failed to create run directory for version " + version, e);
      }
    }

    // Acquire the version manifest for the given version
    VersionManifest manifest;
    try {
      manifest = minecraftRepository.getVersionManifest(version);
    } catch (IOException e) {
      throw new FlintGradleException("IO error while reading version manifest", e);
    }

    // Retrieve the assets store path
    Path assetsPath = runCacheDir.resolve("assets-store");
    String assetTaskName = "download" + manifest.getAssetIndex().getId() + "Assets";
    MinecraftAssetsTask assetsTask = (MinecraftAssetsTask) project.getTasks().findByName(assetTaskName);
    if (assetsTask == null) {
      assetsTask = project.getTasks().create(assetTaskName, MinecraftAssetsTask.class);
      assetsTask.setVersionManifest(manifest);
      assetsTask.setOutputDirectory(assetsPath);
    }

    // Get a natives directory for the run task
    Path nativesDir = runCacheDir.resolve("natives").resolve(manifest.getId());
    if (!Files.isDirectory(nativesDir)) {
      // Try to create the natives directory if it does not exist
      try {
        Files.createDirectories(nativesDir);
      } catch (IOException e) {
        throw new FlintGradleException("Failed to create natives dir for " + manifest.getId(), e);
      }
    }

    // Set up the task with the required arguments
    task.setVersion(version);
    task.setVersionType(manifest.getType().name().toLowerCase());

    // Set arguments
    task.setVersionedArguments(manifest.getArguments());

    // Asset store options
    task.setAssetIndex(manifest.getAssetIndex().getId());
    task.setAssetsPath(assetsPath);

    // Make sure to provide the directory to store natives
    task.setNativesDirectory(nativesDir);

    // Set up java options
    task.setWorkingDir(runDir);
    task.setMain(manifest.getMainClass());

    // Give context to gradle
    task.setGroup(FlintGradlePlugin.MINECRAFT_TASK_GROUP);
    task.setDescription("Runs the minecraft version " + version);

    // Make sure the run task depends on the corresponding assets download task
    task.dependsOn(assetsTask);

    return task;
  }

  /**
   * Retrieves the run configuration name of the given raw name.
   *
   * @param rawConfigurationName The configuration name to transform
   * @return The transformed name
   */
  private String getConfigurationName(String rawConfigurationName) {
    return rawConfigurationName.equals("main") ? "" :
        Character.toUpperCase(rawConfigurationName.charAt(0)) + rawConfigurationName.substring(1);
  }

  /**
   * Resolves the source set or version names to their respective source sets.
   *
   * @param names         The names to resolve
   * @param sourceSets    The source set container with all available source sets
   * @param sourceProject The project the source set originate from
   * @return The resolved source sets
   * @throws IllegalArgumentException If a name can't be resolved
   */
  private Map<String, Map<SourceSet, Project>> resolveNames(Map<String, Set<String>> names, SourceSetContainer sourceSets, Project sourceProject) {
    Map<String, Map<SourceSet, Project>> output = new HashMap<>();

    // Iterate all configurations
    names.forEach((configuration, sourceSetsOrVersions) -> {
      // Iterate all names of the current configuration
      for (String sourceSetOrVersion : sourceSetsOrVersions) {
        // Probe for a source set with the given name
        SourceSet sourceSet = sourceSets.findByName(sourceSetOrVersion);
        if (sourceSet != null) {
          // A source set exactly matching this name found, add it
          output.computeIfAbsent(configuration, (k) -> new HashMap<>()).put(sourceSet, sourceProject);
          continue;
        }

        // No exact match found, try to transform the source set name into a versioned source set name
        String versionedName = "v" + configuration.replace('.', '_');
        sourceSet = sourceSets.findByName(versionedName);
        if (sourceSet == null) {
          // Still not found
          throw new IllegalArgumentException("Tried to resolve source set name " + sourceSetOrVersion +
              ", but no source set with the given name exists nor does a source set with the versioned " +
              "variant exist");
        } else {
          // Found the versioned source set, add it
          output.computeIfAbsent(configuration, (k) -> new HashMap<>()).put(sourceSet, sourceProject);
        }
      }
    });

    return output;
  }
}
