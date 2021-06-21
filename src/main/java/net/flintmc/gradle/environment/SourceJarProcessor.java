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

package net.flintmc.gradle.environment;

import net.flintmc.gradle.util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for working on source jars.
 */
public class SourceJarProcessor {
  private final List<SourceJarAction> actions;

  /**
   * Constructs a new source jar processor without any actions set.
   */
  public SourceJarProcessor() {
    this.actions = new ArrayList<>();
  }

  /**
   * Adds an action to this processor.
   *
   * @param action The action to add
   */
  public void addAction(SourceJarAction action) {
    this.actions.add(action);
  }

  /**
   * Processes the given jar file.
   *
   * @param input  The path to the jar to process
   * @param output The path to the jar to write the processed sources to
   * @throws IllegalStateException If no actions have been added
   * @throws IOException           If an I/O error occurs while processing the jar
   */
  public void process(Path input, Path output) throws IOException {
    if (actions.isEmpty()) {
      throw new IllegalStateException("No actions have been added to this process");
    }

    try (
        ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(input));
        ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(output))
    ) {
      ZipEntry entry;
      // Iterate over every entry
      while ((entry = inputStream.getNextEntry()) != null) {
        outputStream.putNextEntry(entry);

        // We can only remap java files
        if (entry.getName().endsWith(".java")) {
          // Process the given java file
          processStream(inputStream, outputStream);
        } else {
          // Nothing to do, just copy the entire stream
          Util.copyStream(inputStream, outputStream);
        }

        // Make sure to close the entry to finish it
        outputStream.closeEntry();
      }
    }
  }

  /**
   * Processes the given input stream to the given output stream.
   *
   * @param inputStream  The stream to read the data to process from
   * @param outputStream The stream to write the processed data to
   * @throws IOException If an I/O error occurs while processing
   */
  private void processStream(InputStream inputStream, OutputStream outputStream) throws IOException {
    // Create a reader for reading line by line
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    String line;

    StringBuffer snippetBuffer = new StringBuffer();

    // Read all lines available
    while ((line = reader.readLine()) != null) {
      snippetBuffer.append(line).append('\n');
    }

    for (SourceJarAction action : actions) {
      // Process every snippet with every action
      action.process(snippetBuffer);
    }

    // Write the processed snippet
    outputStream.write(snippetBuffer.toString().getBytes(StandardCharsets.UTF_8));
    outputStream.write('\n');
  }
}
