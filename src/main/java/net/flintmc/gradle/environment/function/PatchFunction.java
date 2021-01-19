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

package net.flintmc.gradle.environment.function;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import net.flintmc.gradle.environment.DeobfuscationException;
import net.flintmc.gradle.environment.DeobfuscationUtilities;
import net.flintmc.gradle.environment.mcp.function.MCPFunction;
import net.flintmc.gradle.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class PatchFunction extends Function {
  protected final Path input;
  protected final Path patches;
  protected final Map<String, Path> indexedPatches;

  /**
   * Constructs a new Patch function with the given name and output.
   *
   * @param name    The name of the function
   * @param output  The output of the function
   * @param input   The input of the function
   * @param patches The path to the patches to apply
   */
  public PatchFunction(String name, Path input, Path output, Path patches) {
    super(name, output);
    this.input = input;
    this.patches = patches;
    this.indexedPatches = new HashMap<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void prepare(DeobfuscationUtilities utilities) throws DeobfuscationException {
    try {
      // Collect all patch files
      Files.walk(patches)
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".java.patch"))
          .forEach((patchPath) -> {
            // Calculate the relative path of the patch file
            String relativePath = patches
                .relativize(patchPath)
                .toString()
                .replace(".patch", "")
                .replace("\\", "/");
            indexedPatches.put(relativePath, patchPath);
          });
    } catch(IOException e) {
      throw new DeobfuscationException("IO error occurred while collecting patch files", e);
    }
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
      // Iterate all entries
      while((entry = inputStream.getNextEntry()) != null) {
        outputStream.putNextEntry(entry);

        // Check if there is a patch for the entry if it is not a directory
        if(!entry.isDirectory() && indexedPatches.containsKey(entry.getName())) {
          // Parse the patch file (unified diff format)
          Patch<String> patch =
              UnifiedDiffUtils.parseUnifiedDiff(Files.readAllLines(indexedPatches.get(entry.getName())));

          List<String> originalLines = Util.readAllLines(inputStream);
          List<String> patchedLines;

          try {
            patchedLines = DiffUtils.patch(originalLines, patch);
          } catch(PatchFailedException e) {
            throw new DeobfuscationException("Failed to patch file " + entry.getName(), e);
          }

          Util.writeAllLines(patchedLines, outputStream);
        } else {
          // If there is no patch or the entry is a directory simply copy the entry
          Util.copyStream(inputStream, outputStream);
        }

        // Make sure to close the entry
        outputStream.closeEntry();
      }
    } catch(IOException e) {
      throw new DeobfuscationException("IO error occurred while patching", e);
    }
  }
}
