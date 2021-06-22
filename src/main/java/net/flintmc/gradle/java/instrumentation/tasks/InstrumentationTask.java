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

import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.java.instrumentation.api.InstrumentationException;
import net.flintmc.gradle.java.instrumentation.api.InstrumentationTransformerRegistrator;
import net.flintmc.gradle.java.instrumentation.api.InstrumentationTransformerRegistry;
import net.flintmc.gradle.java.instrumentation.api.context.InstrumentationTransformerContext;
import net.flintmc.gradle.java.instrumentation.api.transformer.InstrumentationTransformer;
import net.flintmc.gradle.java.instrumentation.impl.DefaultInstrumentationTransformerRegistry;
import net.flintmc.gradle.java.instrumentation.impl.context.DefaultInstrumentationTransformerContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetOutput;
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

/**
 * Gradle task to execute post compile class and resource instrumentations.
 */
public class InstrumentationTask extends DefaultTask {

  @Internal
  private SourceSet sourceSet;
  @Internal
  private Configuration configuration;

  @Inject
  public InstrumentationTask() {
  }

  /**
   * Set the gradle SourceSet that should be transformed.
   *
   * @param sourceSet the SourceSet to transform
   */
  public void setSourceSet(final SourceSet sourceSet) {
    this.sourceSet = sourceSet;
  }

  /**
   * Set the gradle dependency configuration from which all transformers are retrieved from.
   *
   * @param configuration the dependency configuration to transform
   */
  public void setConfiguration(Configuration configuration) {
    this.configuration = configuration;
  }

  /**
   * @return the SourceSet that should be transformed
   */
  public SourceSet getSourceSet() {
    return sourceSet;
  }

  /**
   * @return the dependency configuration from which all transformers are retrieved from
   */
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * @return a collection of all class directories of the current SourceSet.
   * @see InstrumentationTask#getSourceSet()
   * @see SourceSetOutput#getClassesDirs()
   */
  private FileCollection getOriginalClassesDirectories() {
    return this.sourceSet.getOutput().getClassesDirs();
  }

  /**
   * @return the single resource directory of the current SourceSet.
   * @see InstrumentationTask#getSourceSet()
   * @see SourceSetOutput#getResourcesDir()
   */
  private File getOriginalResourcesDirectory() {
    return this.sourceSet.getOutput().getResourcesDir();
  }

  /**
   * Performs the transformation
   */
  @TaskAction
  public void executeInstrumentation() {

    //Find classpath for all transformers
    Set<File> files = this.configuration.resolve();
    URL[] urls = new URL[files.size()];

    int i = 0;
    for (File f : files) {
      try {
        urls[i++] = f.toURI().toURL();
      } catch (MalformedURLException e) {
        throw new InstrumentationException("Failed to convert file to URL", e);
      }
    }

    InstrumentationTransformerRegistry registry = new DefaultInstrumentationTransformerRegistry(sourceSet, this.getProject().getPlugins().getPlugin(FlintGradlePlugin.class));
    //Create transformer classpath
    ClassLoader loader = new URLClassLoader(urls, getClass().getClassLoader());

    //load all transformer registrators from created classpath
    //this will be done seperately for every sourceset, so no state will be accidently shared between transformations
    ServiceLoader<InstrumentationTransformerRegistrator> serviceLoader =
        ServiceLoader.load(InstrumentationTransformerRegistrator.class, loader);

    //register all transformers
    for (InstrumentationTransformerRegistrator instrumentationTransformerRegistrator : serviceLoader) {
      instrumentationTransformerRegistrator.initialize(registry);
    }


    //find files to transform
    File originalClassesDir = this.getOriginalClassesDirectories().getSingleFile();
    File instrumentedClassesDir = this.getOutputs().getFiles().getSingleFile();

    for (File file : this.getProject().fileTree(originalClassesDir)) {
      //create relative path from root directory to the transforming file
      String relativePath = file.getAbsolutePath().replaceFirst(originalClassesDir.getAbsolutePath() + "/", "");
      File newFile = new File(instrumentedClassesDir, relativePath);
      try {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
          byte[] bytes = IOUtils.toByteArray(fileInputStream);

          //transform if file is a class
          bytes = transformClassMaybe(file, bytes, registry);
          //TODO: resources are not handled yet. implement that

          //if the transformer returns null bytes, the transformed file will be removed from final compilation
          if (bytes == null) {
            continue;
          }

          //create file in instrumented output directory
          newFile.getParentFile().mkdirs();
          newFile.createNewFile();
          Files.write(newFile.toPath(), bytes);
        }
      } catch (IOException e) {
        throw new InstrumentationException(String.format("Could not instrument file %s in project %s", file, this.getProject().getPath()), e);
      }
    }
  }

  private byte[] transformClassMaybe(File file, byte[] bytes, InstrumentationTransformerRegistry registry) {
    if (!FilenameUtils.getExtension(file.getName()).equals("class")) {
      //not a class, do not touch content
      return bytes;
    }


    //create transformation context that will be used by all transformers in this instrumentation round
    DefaultInstrumentationTransformerContext context = new DefaultInstrumentationTransformerContext(
        this.sourceSet,
        this.getOriginalClassesDirectories(),
        this.getOriginalResourcesDirectory(),
        this.getOutputs().getFiles().getSingleFile(),
        file,
        bytes
    );

    for (Map.Entry<InstrumentationTransformer, Predicate<InstrumentationTransformerContext>> entry : registry.getTransformers().entrySet()) {
      //check if transformer should handle the context
      if (entry.getValue().test(context)) {
        //perform the transformation
        entry.getKey().transform(context);
      }
    }
    return context.getData();
  }
}
