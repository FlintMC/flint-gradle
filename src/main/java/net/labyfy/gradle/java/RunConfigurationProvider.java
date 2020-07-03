package net.labyfy.gradle.java;

import net.labyfy.gradle.LabyfyGradleException;
import net.labyfy.gradle.LabyfyGradlePlugin;
import net.labyfy.gradle.extension.LabyfyGradleExtension;
import net.labyfy.gradle.extension.LabyfyRunsExtension;
import net.labyfy.gradle.minecraft.MinecraftAssetsTask;
import net.labyfy.gradle.minecraft.MinecraftRepository;
import net.labyfy.gradle.minecraft.MinecraftRunTask;
import net.labyfy.gradle.minecraft.data.version.VersionManifest;
import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Utility class for adding run configurations to the root project.
 */
public class RunConfigurationProvider {
    private final Project project;
    private final MinecraftRepository minecraftRepository;
    private final Path runCacheDir;
    private final Map<String, MinecraftRunTask> tasks;
    private final Map<String, Set<SourceSet>> sourceSetsByConfiguration;

    /**
     * Constructs a new {@link RunConfigurationProvider} for the given project.
     *
     * @param project The project to generate configurations on
     * @param minecraftRepository The repository to retrieve minecraft launch information from
     * @param runCacheDir The directory certain thins such as natives and assets are cached in
     */
    public RunConfigurationProvider(Project project, MinecraftRepository minecraftRepository, Path runCacheDir) {
        this.project = project;
        this.minecraftRepository = minecraftRepository;
        this.runCacheDir = runCacheDir;
        this.tasks = new HashMap<>();
        this.sourceSetsByConfiguration = new HashMap<>();
    }

    /**
     * Installs the given source sets into the configurations
     *
     * @param sourceProject The project to use as a source for the source sets
     * @param extension The extension to use as a configuration base
     */
    public void installSourceSets(Project sourceProject, LabyfyGradleExtension extension) {
        LabyfyRunsExtension runs = extension.getRuns();
        SourceSetContainer sourceSets = sourceProject.getExtensions().getByType(SourceSetContainer.class);

        // Retrieve the configurations for which all source sets should be included
        // (unless later explicitly excluded)
        Set<String> allIncludedConfigurations = runs.getAllIncludedConfigurations();

        // Get the special configurations and resolve their source set names
        Map<String, Set<SourceSet>> includedSourceSetsByConfiguration =
                resolveNames(runs.getIncludedSourceSets(), sourceSets);
        Map<String, Set<SourceSet>> excludedSourceSetsByConfiguration =
                resolveNames(runs.getExcludedSourceSets(), sourceSets);

        for(String configuration : allIncludedConfigurations) {
            for(SourceSet sourceSet : sourceSets) {
                // Test if the source set has not been explicitly excluded for the given configuration
                if(!excludedSourceSetsByConfiguration.containsKey(configuration) ||
                        !excludedSourceSetsByConfiguration.get(configuration).contains(sourceSet)) {
                    // The source set has not been excluded, add it
                    sourceSetsByConfiguration.computeIfAbsent(configuration, (k) ->
                            ensureVersionedTasksSetup(configuration, extension.getMinecraftVersions())).add(sourceSet);
                }
            }
        }

        // Iterate all explicitly included source sets
        includedSourceSetsByConfiguration.forEach((configuration, included) -> {
            // Add the source sets
            sourceSetsByConfiguration.computeIfAbsent(configuration,
                    (k) -> ensureVersionedTasksSetup(k, extension.getMinecraftVersions())).addAll(included);
        });
    }

    /**
     * Ensures that all tasks are set up for the given versions and configuration name.
     *
     * @param configuration The name of the configuration
     * @param versions The versions to set runs up for
     * @return The potential classpath of the tasks
     */
    private Set<SourceSet> ensureVersionedTasksSetup(String configuration, Set<String> versions) {
        Set<SourceSet> potentialClasspath = new HashSet<>();
        for(String version : versions) {
            ensureRunTaskSetup(configuration, version, potentialClasspath);
        }

        return potentialClasspath;
    }

    /**
     * Ensures that a run task for the given configuration and version exists.
     *
     * @param configuration The configuration run name
     * @param version The minecraft version of the run task
     * @param potentialClasspath The potential classpath for the task
     */
    private void ensureRunTaskSetup(String configuration, String version, Set<SourceSet> potentialClasspath) {
        // Generate a task name
        String runTaskName = "runClient" + version + getConfigurationName(configuration);
        if(!tasks.containsKey(runTaskName)) {
            // The task does not yet exist, create it
            MinecraftRunTask runTask = createRunTask(runTaskName, version);

            // Set the potential classpath and index the task
            runTask.setPotentialClasspath(potentialClasspath);
            tasks.put(runTaskName, runTask);
        } else {
            // The task exists already
            MinecraftRunTask runTask = tasks.get(runTaskName);
            if(runTask.getPotentialClasspath() != potentialClasspath) {
                // This means something tried to replace the set of source sets afterwards, this would break
                // the discovery mechanism
                throw new IllegalArgumentException("Can't redefine the potential classpath of a minecraft run task");
            }
        }
    }

    /**
     * Creates the minecraft run task with the given name for the given version.
     *
     * @param name The name of the task
     * @param version The minecraft version to create the task for
     * @return The created task
     */
    private MinecraftRunTask createRunTask(String name, String version) {
        // Create the task itself
        MinecraftRunTask task = project.getTasks().create(name, MinecraftRunTask.class);

        // Build a path for the run directory
        Path runDir = project.file("run/" + version).toPath();
        if(!Files.isDirectory(runDir)) {
            // The run directory does not exist, try to create it
            try {
                Files.createDirectories(runDir);
            } catch (IOException e) {
                throw new LabyfyGradleException("Failed to create run directory for version " + version, e);
            }
        }

        // Acquire the version manifest for the given version
        VersionManifest manifest;
        try {
            manifest = minecraftRepository.getVersionManifest(version);
        } catch (IOException e) {
            throw new LabyfyGradleException("IO error while reading version manifest", e);
        }

        // Retrieve the assets store path
        Path assetsPath = runCacheDir.resolve("assets").resolve(manifest.getAssetIndex().getId());
        String assetTaskName = "download" + manifest.getAssetIndex().getId() + "Assets";
        MinecraftAssetsTask assetsTask = (MinecraftAssetsTask) project.getTasks().findByName(assetTaskName);
        if(assetsTask == null) {
            assetsTask = project.getTasks().create(assetTaskName, MinecraftAssetsTask.class);
            assetsTask.setVersionManifest(manifest);
            assetsTask.setOutputDirectory(assetsPath);
        }

        // Get a natives directory for the run task
        Path nativesDir = runCacheDir.resolve("natives").resolve(manifest.getId());
        if(!Files.isDirectory(nativesDir)) {
            // Try to create the natives directory if it does not exist
            try {
                Files.createDirectories(nativesDir);
            } catch (IOException e) {
                throw new LabyfyGradleException("Failed to create natives dir for " + manifest.getId(), e);
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
        task.setGroup(LabyfyGradlePlugin.MINECRAFT_TASK_GROUP);
        task.setDescription("Runs the minecraft version " + version);

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
     * @param names The names to resolve
     * @param sourceSets The source set container with all available source sets
     * @return The resolved source sets
     * @throws IllegalArgumentException If a name can't be resolved
     */
    private Map<String, Set<SourceSet>> resolveNames(Map<String, Set<String>> names, SourceSetContainer sourceSets) {
        Map<String, Set<SourceSet>> output = new HashMap<>();

        // Iterate all configurations
        names.forEach((configuration, sourceSetsOrVersions) -> {
            // Iterate all names of the current configuration
            for(String sourceSetOrVersion : sourceSetsOrVersions) {
                // Probe for a source set with the given name
                SourceSet sourceSet = sourceSets.findByName(sourceSetOrVersion);
                if(sourceSet != null) {
                    // A source set exactly matching this name found, add it
                    output.computeIfAbsent(configuration, (k) -> new HashSet<>()).add(sourceSet);
                    continue;
                }

                // No exact match found, try to transform the source set name into a versioned source set name
                String versionedName = "v" + configuration.replace('.', '_');
                sourceSet = sourceSets.findByName(versionedName);
                if(sourceSet == null) {
                    // Still not found
                    throw new IllegalArgumentException("Tried to resolve source set name " + sourceSetOrVersion +
                            ", but no source set with the given name exists nor does a source set with the versioned " +
                            "variant exist");
                } else {
                    // Found the versioned source set, add it
                    output.computeIfAbsent(configuration, (k) -> new HashSet<>()).add(sourceSet);
                }
            }
        });

        return output;
    }
}
