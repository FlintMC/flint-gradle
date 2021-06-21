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

package net.flintmc.gradle.java.instrumentation.tasks;

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.java.instrumentation.api.InstrumentationTransformerRegistrator;
import net.flintmc.gradle.java.instrumentation.api.InstrumentationTransformerRegistry;
import net.flintmc.gradle.java.instrumentation.api.context.InstrumentationClassTransformerContext;
import net.flintmc.gradle.java.instrumentation.api.transformer.InstrumentationClassTransformer;
import net.flintmc.gradle.java.instrumentation.impl.DefaultInstrumentationTransformerRegistry;
import net.flintmc.gradle.java.instrumentation.impl.context.DefaultInstrumentationClassTransformerContext;
import net.flintmc.gradle.minecraft.data.environment.MinecraftVersion;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;

public class InstrumentationTask extends DefaultTask {

  @Internal
  private SourceSet sourceSet;
  @Internal
  private Configuration configuration;

  @Inject
  public InstrumentationTask() {
  }

  public SourceSet getSourceSet() {
    return sourceSet;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setSourceSet(final SourceSet sourceSet) {
    this.sourceSet = sourceSet;
  }

  @TaskAction
  public void executeInstrumentation() {

    Set<File> files = this.configuration.resolve();
    URL[] urls = new URL[files.size()];

    int i = 0;
    for (File f : files) {
      try {
        urls[i++] = f.toURI().toURL();
      } catch (MalformedURLException e) {
        throw new FlintGradleException("Failed to convert file to URL", e);
      }
    }

    InstrumentationTransformerRegistry registry = new DefaultInstrumentationTransformerRegistry(sourceSet, this.getProject().getPlugins().getPlugin(FlintGradlePlugin.class));
    ClassLoader loader = new URLClassLoader(urls, getClass().getClassLoader());

    ServiceLoader<InstrumentationTransformerRegistrator> serviceLoader =
        ServiceLoader.load(InstrumentationTransformerRegistrator.class, loader);

    for (InstrumentationTransformerRegistrator instrumentationTransformerRegistrator : serviceLoader) {
      instrumentationTransformerRegistrator.initialize(registry);
    }


    File originalClassesDir = this.getOriginalClassesDirectories().getSingleFile();
    File instrumentedClassesDir = this.getOutputs().getFiles().getSingleFile();

    for (File file : this.getProject().fileTree(originalClassesDir)) {
      String relativePath = file.getAbsolutePath().replaceFirst(originalClassesDir.getAbsolutePath() + "/", "");
      File newFile = new File(instrumentedClassesDir, relativePath);
      try {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
          byte[] bytes = IOUtils.toByteArray(fileInputStream);
          bytes = transformClass(file, originalClassesDir, bytes, registry);

          if (bytes == null) {
            continue;
          }
          newFile.getParentFile().mkdirs();
          newFile.createNewFile();
          Files.write(newFile.toPath(), bytes);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
   /* for (String jarPath : this.configuration.getAsPath().split(":")) {
      if (!FilenameUtils.getExtension(jarPath).equalsIgnoreCase("jar")) continue;
      File file = new File(jarPath);
      try {
        ServiceLoader<InstrumentationTransformerRegistrator> serviceLoader = ServiceLoader.load(InstrumentationTransformerRegistrator.class, new URLClassLoader(new URL[]{file.toURI().toURL()}));

        for (InstrumentationTransformerRegistrator instrumentationTransformerRegistrator : serviceLoader) {
          instrumentationTransformerRegistrator.initialize(registry);
        }
        for (File classesDirectory : this.getOriginalClassesDirectories().getFiles()) {
          for (File search : this
              .getOriginalClassesDirectories()
              .getAsFileTree()
              .filter(search -> FilenameUtils.getExtension(search.getName()).equals("class"))) {
            if (search.getPath().startsWith(classesDirectory.getPath())) {


              System.out.println();
            }
          }
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }*/
  }

  private byte[] transformClass(File file, File classesDirectory, byte[] bytes, InstrumentationTransformerRegistry registry) {
    if (!FilenameUtils.getExtension(file.getName()).equals("class")) {
      return bytes;
    }
    String relativePath = file.getPath().replaceFirst(classesDirectory.getPath() + "/", "");
    String name = FilenameUtils.removeExtension(file.getName());
    String packageName = FilenameUtils.removeExtension(relativePath).replace('/', '.');
    packageName = packageName.substring(0, packageName.lastIndexOf(name) - 1);

    DefaultInstrumentationClassTransformerContext context = new DefaultInstrumentationClassTransformerContext(this.sourceSet, this.getOriginalClassesDirectories(), this.getOriginalResourcesDirectory(), name, packageName, file, bytes);

    for (Map.Entry<InstrumentationClassTransformer, Predicate<InstrumentationClassTransformerContext>> entry : registry.getClassTransformers().entrySet()) {
      if (entry.getValue().test(context)) {
        entry.getKey().transform(context);
      }
    }
    return context.getClassData();
  }

  private FileCollection getOriginalClassesDirectories() {
    return this.sourceSet.getOutput().getClassesDirs();
  }

  private File getOriginalResourcesDirectory() {
    return this.sourceSet.getOutput().getResourcesDir();
  }

  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }
}
