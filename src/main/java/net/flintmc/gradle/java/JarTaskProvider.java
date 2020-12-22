package net.flintmc.gradle.java;

import net.flintmc.gradle.extension.FlintGradleExtension;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JarTaskProvider {
  /**
   * Creates jar and publish tasks for the minecraft source sets.
   *
   * @param project   the project to install the tasks in
   * @param extension flint gradle extension to fetch data from
   */
  public void installTasks(Project project, FlintGradleExtension extension) {
    JavaPluginConvention plugin = project.getConvention().getPlugin(JavaPluginConvention.class);
    Set<String> sourceSetsToAdd = extension.getMinecraftVersions()
        .stream()
        .map(name -> 'v' + name.replace('.', '_'))
        .collect(Collectors.toSet());
    sourceSetsToAdd.add("main");
    sourceSetsToAdd.add("internal");

    plugin.getSourceSets()
        .stream()
        .filter(sourceSet -> sourceSetsToAdd.contains(sourceSet.getName()))
        .forEach(sourceSet -> {
          this.addToMainJar(sourceSet, project);
          if (!sourceSet.getName().equals("main")) {
            this.installCompileTask(sourceSet, project);
          }
        });

    createJarBundledInstallerTask(project);
  }

  /**
   * Adds the outputs of other source sets to the main jar.
   *
   * @param sourceSet The source set to add to the main jar
   * @param project   The project to source set belongs to
   */
  @SuppressWarnings("unchecked")
  public void addToMainJar(SourceSet sourceSet, Project project) {
    // Retrieve the jar task producing the fat, bundled jar which will be used
    // in the production environment
    Jar mainJarTask = (Jar) project.getTasks().getByName("jar");

    // Get a place to store properties for a task run
    ExtraPropertiesExtension mainJarTaskProperties = mainJarTask.getExtensions().getExtraProperties();

    // Extract the properties if they exist already
    List<File> serviceInputDirs = mainJarTaskProperties.has("serviceInputDirs") ?
        (List<File>) mainJarTaskProperties.get("serviceInputDirs") : null;
    File serviceMergeDir = mainJarTaskProperties.has("serviceMergeDir") ?
        (File) mainJarTaskProperties.get("serviceMergeDir") : null;

    if (serviceMergeDir == null) {
      // The service merge dir has not been set yet, define it as $buildDir/service-merge
      // and add it as a task input
      serviceMergeDir = new File(project.getBuildDir(), "service-merge");
      mainJarTaskProperties.set("serviceMergeDir", serviceMergeDir);
      mainJarTask.from(serviceMergeDir);
    }

    if (serviceInputDirs == null) {
      // The service input dirs have not been configured yet, create a list to save all
      // directories which should be probed for services in
      serviceInputDirs = new ArrayList<>();
      mainJarTaskProperties.set("serviceInputDirs", serviceInputDirs);

      // Lambda workaround to make variables final
      File finalServiceMergeDir = serviceMergeDir;
      List<File> finalServiceInputDirs = serviceInputDirs;

      mainJarTask.doFirst((self) -> {
        if (finalServiceMergeDir.exists()) {
          // Nuke the merge directory if it exists already
          project.delete(finalServiceMergeDir);
        }

        // Define the full output directory to write service files to and create it
        File serviceOutputDir = new File(finalServiceMergeDir, "META-INF/services");
        if (!serviceOutputDir.mkdirs()) {
          throw new GradleException("Failed to create service merge output directory");
        }

        // Iterate over all possible input dirs to probe for service files
        for (File root : finalServiceInputDirs) {
          File rootServiceDir = new File(root, "META-INF/services");
          if (rootServiceDir.exists()) {
            // Found a directory which potentially contains service files
            File[] descriptorFiles = rootServiceDir.listFiles();
            if (descriptorFiles == null) {
              // Unable to list files in directory, this doesn't have to mean
              // that something went wrong, rather that the operating system
              // did not report any file container to be present in the directory
              continue;
            }

            // Iterate all found files, assume they are service files or directories
            // (shame the developer if they are not)
            for (File serviceDescriptor : descriptorFiles) {
              if (!serviceDescriptor.isFile()) {
                // Ignore things which are not service files, some frameworks
                // use directories inside the service dir
                continue;
              }

              try (
                  // Open a write for appending to a service file in case it exists already
                  FileWriter serviceDescriptorWriter =
                      new FileWriter(new File(serviceOutputDir, serviceDescriptor.getName()), true);
                  // Open a reader to copy from
                  BufferedReader serviceDescriptorReader = new BufferedReader(new FileReader(serviceDescriptor))
              ) {
                String line;
                while ((line = serviceDescriptorReader.readLine()) != null) {
                  // Copy every line over to the service file
                  serviceDescriptorWriter.write(line + "\n");
                }
              } catch (IOException e) {
                throw new GradleException("Failed to merge service files", e);
              }
            }
          }
        }
      });
    }

    // Add the current source outputs as a probe directory for service files
    serviceInputDirs.addAll(sourceSet.getOutput().getFiles());

    // Add the source set output as an input to the main jar task
    mainJarTask.from(sourceSet.getOutput());

    // Configure exclusions so we can merge service files instead of having
    // gradle overwrite them
    mainJarTask.exclude((f) -> {
      // We need the absolute path so we don't exclude wrong stuff
      String path = f.getFile().getAbsolutePath();

      for (File output : sourceSet.getOutput().getFiles()) {
        // Probe if file lies within any source set and is very like a service file
        if (path.startsWith(output.getAbsolutePath()) && path.replace('\\', '/').contains("META-INF/services")) {
          // If so, exclude it
          return true;
        }
      }

      // Merging other stuff is managed by gradle
      return false;
    });

    // Depend on the task, so the single jars are also created when the main jar task
    // is executed. There however is no semantical difference between doing so or not
    // doing so other than the single jars not being created. The main jar task does
    // not really require the other jar tasks to run, as it will collect sources and
    // services on its own.
//    mainJarTask.dependsOn(jarTask);
  }

  /**
   * Installs a task to generate a self installer on the project.
   *
   * @param project The project to add the task on
   */
  public void createJarBundledInstallerTask(Project project) {
    Jar mainJarTask = (Jar) project.getTasks().getByName("jar");
    project.getTasks().register("bundledInstallerJar", Jar.class, (bundledInstallerJarTask) -> {
      // Copy from main jar
      bundledInstallerJarTask.from(project.zipTree(mainJarTask.getOutputs().getFiles().getSingleFile()));
      bundledInstallerJarTask.getArchiveClassifier().set("bundled-installer");
      bundledInstallerJarTask.dependsOn(mainJarTask);

      // Create a detached configuration to resolve artifacts from
      Configuration bundledInstallerConfiguration = project.getConfigurations().detachedConfiguration(
          project.getDependencies().create("net.flintmc.installer:frontend-gui:1.1.6")
      );

      Set<Object> inputs = new HashSet<>();

      for (File file : bundledInstallerConfiguration) {
        // Compute the inputs
        if (file.isDirectory()) {
          // Add directories directly
          inputs.add(file);
        } else {
          // Add files as zip trees
          inputs.add(project.zipTree(file));
        }
      }

      // Include the configuration in the jar
      bundledInstallerJarTask.from(inputs);
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
