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

package net.flintmc.gradle.java.compile;

import net.flintmc.gradle.java.exec.JavaExecutionResult;
import net.flintmc.gradle.util.Util;
import org.gradle.api.Project;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

/**
 * Utility class for compiling Java source code using the Gradle configured compiler.
 */
public class JavaCompileHelper {
  private final Project project;

  /**
   * Creates a new Java compile helper using the given project as a base for accessing
   * the gradle configured compiler.
   *
   * @param project The project to use for accessing the compile
   */
  public JavaCompileHelper(Project project) {
    this.project = project;
  }

  /**
   * Compiles the given source directory with the given classpath writing into the given jar.
   *
   * @param source    The source to directory to compile
   * @param classpath The classpath to pass to the compiler
   * @param outputJar The jar to write to
   * @return The result of the compilation and packaging
   * @throws IOException If an I/O error occurs while compiling or packaging
   */
  public JavaExecutionResult compile(Path source, List<Path> classpath, Path outputJar) throws IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new IOException("A JDK is required in order to use the compiler");
    }

    // Get a place to put the output
    Path compileOutputDir = Util.temporaryDir();

    try {
      // Save the result of the compiler execution
      JavaExecutionResult compilerResult;

      try (
          ByteArrayOutputStream compileOutputStream = new ByteArrayOutputStream();
          ByteArrayOutputStream compileErrorStream = new ByteArrayOutputStream()
      ) {
        // Set up javac arguments
        List<String> arguments = new ArrayList<>();

        // Set the classpath
        arguments.add("-classpath");
        arguments.add(project.files(classpath).getAsPath());

        // Make sure to read the input files as UTF-8 to prevent weird encoding issues
        arguments.add("-encoding");
        arguments.add("utf8");

        // Set the output directory
        arguments.add("-d");
        arguments.add(compileOutputDir.toString());

        // Add all source files to the end of the commandline
        for (File file : project.fileTree(source).filter(file -> file.getName().endsWith(".java"))) {
          arguments.add(file.getAbsolutePath());
        }

        int exitCode =
            compiler.run(null, compileOutputStream, compileErrorStream, arguments.toArray(new String[0]));

        // Save the summary
        compilerResult = new JavaExecutionResult(
            exitCode,
            compileOutputStream.toString("UTF-8"),
            compileErrorStream.toString("UTF-8")
        );

        if (exitCode != 0) {
          // Don't attempt to package the jar if the compiler failed
          return compilerResult;
        }
      }

      // Make sure to create the parent directories
      if (!Files.isDirectory(outputJar.getParent())) {
        Files.createDirectories(outputJar.getParent());
      }

      // Collect all files to package into the jar
      Map<String, Path> jarContent = new HashMap<>();
      jarContent.putAll(relativeChildren(source));
      jarContent.putAll(relativeChildren(compileOutputDir));

      try (JarOutputStream outputStream = new JarOutputStream(Files.newOutputStream(outputJar))) {
        // Iterate all files that should be put into the jar
        for (Map.Entry<String, Path> entry : jarContent.entrySet()) {
          // Extract the values from the map
          String relativePath = entry.getKey().replace('\\', '/');
          Path absolutePath = entry.getValue();

          // Write the entry
          JarEntry jarEntry = new JarEntry(relativePath);
          outputStream.putNextEntry(jarEntry);

          try (InputStream inputStream = Files.newInputStream(absolutePath)) {
            // Copy the entire entry
            Util.copyStream(inputStream, outputStream);
          }

          // Make sure to close the entry
          outputStream.closeEntry();
        }
      }

      return compilerResult;
    } finally {
      // Clean up the compiler output, it is not required anymore
      Util.nukeDirectory(compileOutputDir, true);
    }
  }

  /**
   * Recursively collects all files of the given root and maps them
   * to a pair of absolute and relative paths. This function excludes all java source files.
   *
   * @param root The root path to search for files
   * @return A map of all files in the root recursively mapped to their absolute and relative paths
   * @throws IOException If an I/O error occurs while walking the file tree
   */
  private Map<String, Path> relativeChildren(Path root) throws IOException {
    Map<String, Path> output = new HashMap<>();

    try (Stream<Path> stream = Files.walk(root)) {
      stream.filter(file -> !file.getFileName().toString().endsWith(".java"))
          .filter(Files::isRegularFile)
          // Use the path as the key and the path relative to the root as the value
          .forEach((path) -> output.put(root.relativize(path).toString(), path));
    }

    return output;
  }
}
