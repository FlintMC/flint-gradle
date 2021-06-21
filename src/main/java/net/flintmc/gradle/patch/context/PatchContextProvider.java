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

import java.io.IOException;
import java.util.List;
import net.flintmc.gradle.patch.PatchSingle;

/** Represents a context provider for patch files. */
public interface PatchContextProvider {

  /**
   * Retrieves a collection with all lines of the given {@code patch} file.
   *
   * @param patch A single patch file.
   * @return A collection with all lines of the given {@code patch} file.
   * @throws IOException If an I/O error has occurred.
   */
  List<String> getData(PatchSingle patch) throws IOException;

  /**
   * Changes the lines of the given {@code patch}.
   *
   * @param patch A single patch file.
   * @param data The new collection of lines for the patch file.
   * @throws IOException If an I/O error has occurred.
   */
  void setData(PatchSingle patch, List<String> data) throws IOException;

  /**
   * Sets the given patch file as failed.
   *
   * @param patch A single patch file.
   * @param lines The new collection of lines for the patch file.
   * @throws IOException If an I/O error has occurred.
   */
  void setFailed(PatchSingle patch, List<String> lines) throws IOException;
}
