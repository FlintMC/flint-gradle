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

package net.flintmc.gradle.patch;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.flintmc.gradle.patch.function.PatchSupplier;

public final class PatchFile {

  private final PatchSupplier supplier;
  private final boolean requiresFurtherProcessing;

  /**
   * Constructs a new {@link PatchFile} with the given parameters.
   *
   * @param supplier The supplier for the patch.
   * @param requiresFurtherProcessing {@code true} if further processing is required, otherwise
   *     {@code false}.
   */
  private PatchFile(PatchSupplier supplier, boolean requiresFurtherProcessing) {
    this.supplier = supplier;
    this.requiresFurtherProcessing = requiresFurtherProcessing;
  }

  /**
   * Retrieves a new {@link PatchFile}
   *
   * @param content The content to open.
   * @return A new patch file.
   */
  public static PatchFile from(String content) {
    return from(content.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Retrieves a new {@link PatchFile} from the given {@code data} byte array.
   *
   * @param data The byte array to open.
   * @return A new patch file.
   */
  public static PatchFile from(byte[] data) {
    return new PatchFile(() -> new ByteArrayInputStream(data), false);
  }

  /**
   * Retrieves a new {@link PatchFile} from the given {@link File}.
   *
   * @param file The file to open.
   * @return A new patch file.
   */
  public static PatchFile from(File file) {
    return new PatchFile(() -> new FileInputStream(file), true);
  }

  /**
   * Opens the {@link InputStream} of this patch file.
   *
   * @return The input stream of this patch file.
   * @throws IOException If an I/O error has occurred.
   */
  public InputStream openStream() throws IOException {
    return this.supplier.get();
  }

  /**
   * Whether if further processing is required.
   *
   * @return {@code true} if further processing is required, otherwise {@code false}.
   */
  public boolean requiresFurtherProcessing() {
    return this.requiresFurtherProcessing;
  }
}
