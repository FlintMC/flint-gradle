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

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.extension.FlintSelfInstallerExtension;
import net.flintmc.gradle.json.JsonConverter;
import net.flintmc.gradle.manifest.dev.DevelopmentStaticFiles;
import net.flintmc.installer.frontend.gui.InstallBundle;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;

import java.io.*;
import java.util.*;
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
        .map(name -> 'v' + name.getVersion().replace('.', '_'))
        .collect(Collectors.toSet());
    sourceSetsToAdd.add("main");
    sourceSetsToAdd.add("internal");

    plugin.getSourceSets()
        .stream()
        .filter(sourceSet -> sourceSetsToAdd.contains(sourceSet.getName()))
        .forEach(sourceSet -> {
          this.addToMainJar(sourceSet, project);
          if(!sourceSet.getName().equals("main")) {
            this.installCompileTask(sourceSet, project);
          }
        });

    createJarBundledInstallerTask(project, extension.getSelfInstaller());
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

    if(serviceMergeDir == null) {
      // The service merge dir has not been set yet, define it as $buildDir/service-merge
      // and add it as a task input
      serviceMergeDir = new File(project.getBuildDir(), "service-merge");
      mainJarTaskProperties.set("serviceMergeDir", serviceMergeDir);
      mainJarTask.from(serviceMergeDir);
    }

    if(serviceInputDirs == null) {
      // The service input dirs have not been configured yet, create a list to save all
      // directories which should be probed for services in
      serviceInputDirs = new ArrayList<>();
      mainJarTaskProperties.set("serviceInputDirs", serviceInputDirs);

      // Lambda workaround to make variables final
      File finalServiceMergeDir = serviceMergeDir;
      List<File> finalServiceInputDirs = serviceInputDirs;

      mainJarTask.doFirst((self) -> {
        if(finalServiceMergeDir.exists()) {
          // Nuke the merge directory if it exists already
          project.delete(finalServiceMergeDir);
        }

        // Define the full output directory to write service files to and create it
        File serviceOutputDir = new File(finalServiceMergeDir, "META-INF/services");
        if(!serviceOutputDir.mkdirs()) {
          throw new GradleException("Failed to create service merge output directory");
        }

        // Iterate over all possible input dirs to probe for service files
        for(File root : finalServiceInputDirs) {
          File rootServiceDir = new File(root, "META-INF/services");
          if(rootServiceDir.exists()) {
            // Found a directory which potentially contains service files
            File[] descriptorFiles = rootServiceDir.listFiles();
            if(descriptorFiles == null) {
              // Unable to list files in directory, this doesn't have to mean
              // that something went wrong, rather that the operating system
              // did not report any file container to be present in the directory
              continue;
            }

            // Iterate all found files, assume they are service files or directories
            // (shame the developer if they are not)
            for(File serviceDescriptor : descriptorFiles) {
              if(!serviceDescriptor.isFile()) {
                // Ignore things which are not service files, some frameworks
                // use directories inside the service dir
                continue;
              }

              try(
                  // Open a write for appending to a service file in case it exists already
                  FileWriter serviceDescriptorWriter =
                      new FileWriter(new File(serviceOutputDir, serviceDescriptor.getName()), true);
                  // Open a reader to copy from
                  BufferedReader serviceDescriptorReader = new BufferedReader(new FileReader(serviceDescriptor))
              ) {
                String line;
                while((line = serviceDescriptorReader.readLine()) != null) {
                  // Copy every line over to the service file
                  serviceDescriptorWriter.write(line + "\n");
                }
              } catch(IOException e) {
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

      for(File output : sourceSet.getOutput().getFiles()) {
        // Probe if file lies within any source set and is very like a service file
        if(path.startsWith(output.getAbsolutePath()) && path.replace('\\', '/').contains("META-INF/services")) {
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
   * @param project   The project to add the task on
   * @param extension The extension to retrieve the configuration from
   */
  public void createJarBundledInstallerTask(Project project, FlintSelfInstallerExtension extension) {
    if(!extension.isEnabled()) {
      // Nothing to do
      return;
    }

    Jar mainJarTask = (Jar) project.getTasks().getByName("jar");
    project.getTasks().create("bundledInstallerJar", Jar.class, (bundledInstallerJarTask) -> {
      bundledInstallerJarTask.getArchiveClassifier().set("bundled-installer");
      bundledInstallerJarTask.setGroup("build");
      bundledInstallerJarTask.dependsOn(mainJarTask);

      Map<String, String> bundle = new HashMap<>();

      Map<String, File> staticFiles = DevelopmentStaticFiles.getFor(project);
      if(staticFiles != null) {
        staticFiles.forEach((localPath, file) -> {
          String inJarPath = "resources/" + localPath.substring(0, localPath.lastIndexOf('/'));

          // Package the file into the resources folder
          bundledInstallerJarTask.from(file, (spec) -> spec.into(inJarPath));
          bundle.put(localPath, inJarPath);
        });
      }

      // Package the main jar
      File mainJar = mainJarTask.getOutputs().getFiles().getSingleFile();
      String inJarPath = "resources/" + mainJar.getName();
      bundle.put(mainJar.getName(), inJarPath);

      // Include certain files from main jar
      bundledInstallerJarTask.from(project.zipTree(mainJar), (spec) -> spec.include("manifest.json"));

      bundledInstallerJarTask.from(mainJar, (spec) -> spec.into("resources"));

      // Create the bundle.json file
      File bundleFile = new File(project.getBuildDir(), "tmp/bundledJar/bundle.json");
      if(!bundleFile.getParentFile().isDirectory() && !bundleFile.getParentFile().mkdirs()) {
        throw new FlintGradleException(
            "Failed to create bundled jar dir " + bundleFile.getParentFile().getAbsolutePath());
      }

      try {
        // Write the bundle file
        JsonConverter.OBJECT_MAPPER.writeValue(bundleFile, new InstallBundle(bundle));
      } catch(IOException e) {
        throw new FlintGradleException("Failed to write bundle.json", e);
      }

      // Include the bundle file
      bundledInstallerJarTask.from(bundleFile);

      // Create a detached configuration to resolve artifacts from
      Configuration bundledInstallerConfiguration = project.getConfigurations().detachedConfiguration(
          project.getDependencies().create(extension.getDependencyNotation())
      );

      Set<Object> inputs = new HashSet<>();

      for(File file : bundledInstallerConfiguration) {
        // Compute the inputs
        if(file.isDirectory()) {
          // Add directories directly
          inputs.add(file);
        } else {
          // Add files as zip trees
          inputs.add(project.zipTree(file));
        }
      }

      // Include the configuration in the jar
      bundledInstallerJarTask.from(inputs);

      // Configure main class
      bundledInstallerJarTask.manifest((manifest) -> {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Main-Class", extension.getMainClass());

        manifest.attributes(attributes);
      });

      // Make "build" depend on the bundled installer
      project.getTasks().getByName("build").dependsOn(bundledInstallerJarTask);
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
