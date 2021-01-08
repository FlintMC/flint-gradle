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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class InjectFunction extends MCPFunction {
  private final Path input;

  /**
   * Constructs a new Inject function with the given name, input and output.
   *
   * @param name   The name of the function
   * @param output The output of the function
   * @param input  The input of the function
   */
  public InjectFunction(String name, Path input, Path output) {
    super(name, output);
    this.input = input;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(DeobfuscationUtilities utilities) throws DeobfuscationException {
    try(
        ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(input));
        ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(output))
    ) {
      ZipEntry entry;

      // Iterate over every entry
      while((entry = inputStream.getNextEntry()) != null) {
        // Copy the entry one to one
        outputStream.putNextEntry(entry);
        Util.copyStream(inputStream, outputStream);

        // Make sure to close the entry
        outputStream.closeEntry();
      }

      // Add our marker entry
      entry = new ZipEntry(".mcp-processed");
      outputStream.putNextEntry(entry);
      outputStream.closeEntry();
    } catch(IOException e) {
      throw new DeobfuscationException("Failed to execute inject function named " + name, e);
    }
  }
}
