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

package net.flintmc.gradle.environment.mcp.function;

import net.flintmc.gradle.environment.DeobfuscationException;
import net.flintmc.gradle.environment.DeobfuscationUtilities;
import net.flintmc.gradle.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class StripFunction extends MCPFunction {
  private final Path mappings;
  private final Path input;
  private final boolean whitelist;
  private final Set<String> classList;


  /**
   * Constructs a new Strip function with the given name, input and output.
   *
   * @param name      The name of the function
   * @param mappings  Mappings to use for determining what to strip
   * @param input     The input of the function
   * @param output    The output of the function
   * @param whitelist If the strip function should operate in whitelist mode
   */
  public StripFunction(String name, Path mappings, Path input, Path output, boolean whitelist) {
    super(name, output);
    this.mappings = mappings;
    this.input = input;
    this.whitelist = whitelist;
    this.classList = new HashSet<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void prepare(DeobfuscationUtilities utilities) throws DeobfuscationException {
    // Open the mappings file
    try (BufferedReader reader = Files.newBufferedReader(mappings)) {
      String line;
      while ((line = reader.readLine()) != null) {
        // If the line is not a class mapping, skip it
        if (line.startsWith("\t")) {
          continue;
        }

        // Parse the mapping line
        classList.add(line.split(" ", 2)[0] + ".class");
      }
    } catch (IOException e) {
      throw new DeobfuscationException("IO error occurred while reading the mappings file", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(DeobfuscationUtilities utilities) throws DeobfuscationException {
    try (
        ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(input));
        ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(output))
    ) {
      ZipEntry entry;

      // Iterate all entries
      while ((entry = inputStream.getNextEntry()) != null) {
        if (shouldStripEntry(entry)) {
          // The entry should be stripped from the jar,
          // simply don't write it to the output stream
          continue;
        }

        // Copy the entry
        outputStream.putNextEntry(entry);
        Util.copyStream(inputStream, outputStream);
        outputStream.closeEntry();
      }
    } catch (IOException e) {
      throw new DeobfuscationException("IO error occurred while stripping jar", e);
    }
  }

  /**
   * Determines if the given entry should be stripped from the jar.
   *
   * @param entry The entry to check
   * @return {@code true} if the entry should be kept, {@code false} otherwise
   */
  private boolean shouldStripEntry(ZipEntry entry) {
    return entry.isDirectory() ||
        (classList.contains(entry.getName()) != whitelist && !entry.getName().startsWith("assets/"));
  }
}
