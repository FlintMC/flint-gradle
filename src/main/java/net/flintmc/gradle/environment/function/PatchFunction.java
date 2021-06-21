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

import com.cloudbees.diff.PatchException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import net.flintmc.gradle.environment.DeobfuscationException;
import net.flintmc.gradle.environment.DeobfuscationUtilities;
import net.flintmc.gradle.patch.PatchContextual;
import net.flintmc.gradle.patch.PatchFile;
import net.flintmc.gradle.patch.context.ZipPatchContextProvider;
import net.flintmc.gradle.patch.report.HunkReport;
import net.flintmc.gradle.patch.report.PatchReport;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class PatchFunction extends Function {
  private static final Logger LOGGER = Logging.getLogger(PatchFunction.class);

  protected final Path input;
  protected final Path patches;
  protected final Map<String, Path> indexedPatches;

  /**
   * Constructs a new Patch function with the given name and output.
   *
   * @param name The name of the function
   * @param output The output of the function
   * @param input The input of the function
   * @param patches The path to the patches to apply
   */
  public PatchFunction(String name, Path input, Path output, Path patches) {
    super(name, output);
    this.input = input;
    this.patches = patches;
    this.indexedPatches = new HashMap<>();
  }

  /** {@inheritDoc} */
  @Override
  public void prepare(DeobfuscationUtilities utilities) throws DeobfuscationException {
    try {
      // Collect all patch files
      Files.walk(patches)
          .filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".java.patch"))
          .forEach(
              (patchPath) -> {
                // Calculate the relative path of the patch file
                String relativePath =
                    patches
                        .relativize(patchPath)
                        .toString()
                        .replace(".patch", "")
                        .replace("\\", "/");
                indexedPatches.put(relativePath, patchPath);
              });
    } catch (IOException e) {
      throw new DeobfuscationException("IO error occurred while collecting patch files", e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void execute(DeobfuscationUtilities utilities) throws DeobfuscationException {
    try (ZipFile zipFile = new ZipFile(this.input.toFile())) {
      ZipPatchContextProvider zipPatchContextProvider = new ZipPatchContextProvider(zipFile);

      Boolean reduce =
          Files.walk(this.patches)
              .filter(
                  patch ->
                      Files.isRegularFile(patch)
                          && patch.getFileName().toString().endsWith(".patch"))
              .map(
                  patch -> {
                    boolean success = true;
                    PatchContextual patchContextual =
                        PatchContextual.create(
                            PatchFile.from(patch.toFile()), zipPatchContextProvider);
                    patchContextual.setCanonicalization(false, false);
                    patchContextual.setMaximalAttempt(10);

                    String name =
                        patch
                            .toFile()
                            .getAbsolutePath()
                            .substring(this.patches.toFile().getAbsolutePath().length() + 1);

                    try {
                      LOGGER.info("Apply Patch: {}", name);

                      List<PatchReport> result = patchContextual.patch(false);

                      for (int i = 0; i < result.size(); i++) {
                        PatchReport report = result.get(i);

                        if (!report.getStatus().isSuccess()) {
                          LOGGER.info("Apply Patch: {}", name);
                          success = false;

                          for (int j = 0; j < report.getHunkReports().size(); j++) {
                            HunkReport hunkReport = report.getHunkReports().get(j);

                            if (hunkReport.hasFailed()) {
                              if (hunkReport.getFailure() == null) {
                                LOGGER.error(
                                    "\tHunk #{} Failed @{} Fuzz: {}",
                                    hunkReport.getHunkIdentifier(),
                                    hunkReport.getIndex(),
                                    hunkReport.getFailure());
                              } else {
                                LOGGER.error(
                                    "\tHunk #{} Failed: {}",
                                    hunkReport.getHunkIdentifier(),
                                    hunkReport.getFailure().getMessage());
                              }
                            }
                          }
                        }
                      }

                    } catch (PatchException exception) {
                      LOGGER.error("\tPatch Name: " + name);
                      LOGGER.error("\t\t" + exception.getMessage());
                    } catch (IOException exception) {
                      LOGGER.error("Patch Name: " + name);
                      throw new UncheckedIOException(exception);
                    }

                    return success;
                  })
              .reduce(true, (a, b) -> a && b);

      if (reduce) {
        zipPatchContextProvider.save(this.output.toFile());
      }

    } catch (IOException exception) {
      throw new DeobfuscationException(exception);
    }
  }
}
