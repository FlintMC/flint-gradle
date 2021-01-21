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

package net.flintmc.gradle.patcher;

import com.cloudbees.diff.Diff;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.flintmc.gradle.util.Util;
import org.apache.commons.io.IOUtils;
import org.apache.groovy.json.internal.IO;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.*;

/** This task generates patches. */
public class GeneratePatchesTask extends DefaultTask {

  public static final String NAME = "generatePatches";
  private static final int CONTEXT_LINES = 3;

  @InputFile
  private File cleanSource;

  @InputFile
  private File modifiedSource;

  @OutputDirectory
  private File patches;

  @Input
  @Optional
  private String originalPrefix;

  @Input
  @Optional
  private String modifiedPrefix;

  /**
   * Executes this tasks.
   *
   * @throws IOException Thrown when an I/O error has occurred.
   */
  @TaskAction
  public void execute() throws IOException {

    Set<Path> paths = new HashSet<>();

    Files.walk(this.patches.toPath()).filter(Files::isRegularFile).forEach(paths::add);

    File sourcesDirectory = new File("build/source/");

    if (!sourcesDirectory.exists()) {
      sourcesDirectory.mkdirs();
    }

    File cleanOutput = createZipFile(sourcesDirectory, "clean.zip");

    Util.toZip(this.cleanSource.toPath(), cleanOutput.toPath());

    File modifiedOutput = createZipFile(sourcesDirectory, "modified.zip");

    Util.toZip(this.modifiedSource.toPath(), modifiedOutput.toPath());

    try (ZipFile clean = new ZipFile(cleanOutput)) {
      ZipFile modified = new ZipFile(modifiedOutput);

      Set<String> cleanEntries =
          Collections.list(clean.entries()).stream()
              .filter(entry -> !entry.isDirectory())
              .map(ZipEntry::getName)
              .collect(Collectors.toSet());

      Set<String> modifiedEntries =
          Collections.list(clean.entries()).stream()
              .filter(entry -> !entry.isDirectory())
              .map(ZipEntry::getName)
              .collect(Collectors.toSet());

      for (String cleanEntry : cleanEntries) {
        ZipEntry newEntry = modified.getEntry(cleanEntry);

        String differences =
            createPatch(
                cleanEntry,
                clean.getInputStream(clean.getEntry(cleanEntry)),
                newEntry == null ? null : modified.getInputStream(newEntry));
        modifiedEntries.remove(cleanEntry);

        if (differences != null) {
          File patch = new File(this.patches, cleanEntry + ".patch");
          this.writePatch(patch, differences);
          paths.remove(patch.toPath());
        }
      }

      for (String modifiedEntry : modifiedEntries) {

        String differences =
            this.createPatch(
                modifiedEntry, null, modified.getInputStream(modified.getEntry(modifiedEntry)));

        if (differences != null) {
          File patch = new File(this.patches, modifiedEntry + ".patch");
          this.writePatch(patch, differences);
          paths.remove(patch.toPath());
        }
      }
    }

    paths.forEach(path -> path.toFile().delete());
    List<File> directories =
        Files.walk(this.patches.toPath())
            .filter(Files::isDirectory)
            .map(Path::toFile)
            .collect(Collectors.toList());

    Collections.reverse(directories);
    directories.forEach(
        file -> {
          if (file.list().length == 0) {
            file.delete();
          }
        });
  }

  private File createZipFile(File directory, String fileName) throws IOException {
    File file = new File(directory, fileName);

    if(file.exists()) {
      file.delete();
    }
    file.createNewFile();

    return file;
  }

  /**
   * Creates a new patch file.
   *
   * @param name The name of the patch file.
   * @param original The original input stream.
   * @param modified The modified input stream.
   * @return A string with the differences of the original and modified input stream.
   * @throws IOException Thrown when an I/O error has occurred.
   */
  private String createPatch(String name, InputStream original, InputStream modified)
      throws IOException {

    String originalRelative = original == null ? "/dev/null" : this.originalPrefix + name;
    String modifiedRelative = modified == null ? "/dev/null" : this.modifiedPrefix + name;
    String originalData =
        original == null ? "" : new String(IOUtils.toByteArray(original), StandardCharsets.UTF_8);
    String modifiedData =
        modified == null ? "" : new String(IOUtils.toByteArray(modified), StandardCharsets.UTF_8);

    Diff differences =
        Diff.diff(new StringReader(originalData), new StringReader(modifiedData), false);

    if (!differences.isEmpty()) {
      return differences
          .toUnifiedDiff(
              originalRelative,
              modifiedRelative,
              new StringReader(originalData),
              new StringReader(modifiedData),
              CONTEXT_LINES)
          .replace("\r?\n", "\n");
    }

    return null;
  }

  /**
   * Writes a patch file.
   *
   * @param patch The location for the patch file.
   * @param differences The differences between the clean and modified sources.
   */
  private void writePatch(File patch, String differences) {
    File parent = patch.getParentFile();

    if (!parent.exists()) {
      parent.mkdirs();
    }

    try(FileOutputStream fileOutputStream = new FileOutputStream(patch)) {
      IOUtils.write(differences, fileOutputStream);
    } catch (IOException exception) {
      exception.printStackTrace();
    }

  }

  /**
   * Retrieves the location where the clean source code is located.
   *
   * @return The location where the clean source code is located.
   */
  public File getCleanSource() {
    return cleanSource;
  }

  /**
   * Changes the location of the clean source code.
   *
   * @param cleanSource The new location for the clean source code.
   */
  public void setCleanSource(File cleanSource) {
    this.cleanSource = cleanSource;
  }

  /**
   * Retrieves the location where the modified source code is located.
   *
   * @return The location where the modified source code is located.
   */
  public File getModifiedSource() {
    return modifiedSource;
  }

  /**
   * Changes the location of the modified source code.
   *
   * @param modifiedSource The new location for the modified source code.
   */
  public void setModifiedSource(File modifiedSource) {
    this.modifiedSource = modifiedSource;
  }

  /**
   * Retrieves the location where the patches are located.
   *
   * @return The location where the patches are located.
   */
  public File getPatches() {
    return patches;
  }

  /**
   * Changes the location where the patches are located.
   *
   * @param patches The new location where the patches.
   */
  public void setPatches(File patches) {
    this.patches = patches;
  }

  /**
   * Retrieves the original prefix for the patch files.
   *
   * @return The original prefix for the patch files.
   */
  public String getOriginalPrefix() {
    return originalPrefix;
  }

  /**
   * Changes the original prefix for the patch files.
   *
   * @param originalPrefix The new original prefix for the patch files.
   */
  public void setOriginalPrefix(String originalPrefix) {
    this.originalPrefix = originalPrefix;
  }

  /**
   * Retrieves the modified prefix for the patch files.
   *
   * @return The modified prefix for the patch files.
   */
  public String getModifiedPrefix() {
    return modifiedPrefix;
  }

  /**
   * Changes the modified prefix for the patch files.
   *
   * @param modifiedPrefix The new modified prefix for the patch files.
   */
  public void setModifiedPrefix(String modifiedPrefix) {
    this.modifiedPrefix = modifiedPrefix;
  }
}
