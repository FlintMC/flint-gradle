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

package net.flintmc.gradle.patch.context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import net.flintmc.gradle.patch.PatchSingle;
import net.flintmc.gradle.patch.state.PatchMode;
import net.flintmc.gradle.util.Util;
import org.apache.commons.io.IOUtils;

public class ZipPatchContextProvider implements PatchContextProvider {

  private final ZipFile zipFile;
  private final Map<String, List<String>> modified;
  private final Map<String, List<String>> rejects;
  private final Set<String> delete;
  private final Map<String, byte[]> binary;

  public ZipPatchContextProvider(ZipFile zipFile) {
    this.zipFile = zipFile;
    this.modified = new HashMap<>();
    this.rejects = new HashMap<>();
    this.delete = new HashSet<>();
    this.binary = new HashMap<>();
  }

  /** {@inheritDoc} */
  @Override
  public List<String> getData(PatchSingle patch) throws IOException {

    if (this.modified.containsKey(patch.getTargetPath())) {
      return this.modified.get(patch.getTargetPath());
    }

    ZipEntry entry = zipFile.getEntry(patch.getTargetPath());

    if (entry == null || patch.isBinary()) {
      return null;
    }

    try (InputStream inputStream = zipFile.getInputStream(entry)) {
      return new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
          .lines()
          .collect(Collectors.toList());
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setData(PatchSingle patch, List<String> data) throws IOException {
    if (patch.getPatchMode() == PatchMode.DELETE || (patch.isBinary() && patch.getHunks().length == 0)) {
      this.delete.add(patch.getTargetPath());
      this.binary.remove(patch.getTargetPath());
      this.modified.remove(patch.getTargetPath());
    } else {

      this.delete.remove(patch.getTargetPath());

      if (patch.isBinary()) {
        this.binary.put(patch.getTargetPath(), Util.deserializeLines(patch.getHunks()[0].lines));
        this.modified.remove(patch.getTargetPath());
      } else {

        if (!patch.isNoEndingNewline()) {
          data.add("");
        }

        this.modified.put(patch.getTargetPath(), data);
        this.binary.remove(patch.getTargetPath());
      }
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setFailed(PatchSingle patch, List<String> lines) throws IOException {
    this.rejects.put(patch.getTargetPath() + ".rej", lines);
  }

  /**
   * Saves the given file.
   *
   * @param file The file to be saved.
   * @throws IOException If an I/O error has occurred.
   */
  public void save(File file) throws IOException {
    File parent = file.getParentFile();

    if (!parent.exists()) {
      parent.mkdirs();
    }

    try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
      save(zipOutputStream);
    }
  }

  /** @see #save(File) */
  private Set<String> save(ZipOutputStream zipOutputStream) throws IOException {
    Set<String> files = new HashSet<>();

    for (Enumeration<? extends ZipEntry> entries = this.zipFile.entries();
        entries.hasMoreElements(); ) {
      files.add(entries.nextElement().getName());
    }

    files.addAll(this.modified.keySet());
    files.addAll(this.binary.keySet());
    files.remove(this.delete);

    List<String> sorted = new ArrayList<>(files);
    Collections.sort(sorted);

    for (String key : sorted) {
      this.putNextEntry(zipOutputStream, key);

      if (this.binary.containsKey(key)) {
        zipOutputStream.write(this.binary.get(key));
      } else if (this.modified.containsKey(key)) {
        zipOutputStream.write(
            String.join("\n", this.modified.get(key)).getBytes(StandardCharsets.UTF_8));
      } else {
        try (InputStream inputStream = this.zipFile.getInputStream(this.zipFile.getEntry(key))) {
          IOUtils.copy(inputStream, zipOutputStream);
        }
      }

      zipOutputStream.closeEntry();
    }

    return files;
  }

  private void putNextEntry(ZipOutputStream zipOutputStream, String name) throws IOException {
    ZipEntry entry = new ZipEntry(name);
    entry.setTime(0);
    zipOutputStream.putNextEntry(entry);
  }
}
