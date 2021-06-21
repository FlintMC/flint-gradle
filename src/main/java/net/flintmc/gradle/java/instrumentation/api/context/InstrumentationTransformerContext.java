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

package net.flintmc.gradle.java.instrumentation.api.context;

import net.flintmc.gradle.java.instrumentation.tasks.InstrumentationTask;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;

import java.io.File;

/**
 * Util class to pass information of the current instrumentation round to a set of transformers.
 */
public interface InstrumentationTransformerContext {

  SourceSet getSourceSet();

  FileCollection getOriginalClassDirectories();

  File getOriginalResourceDirectory();

  File getInstrumentedClassDirectory();

  /**
   * Should only be used to read from.
   * Directly writing to this file might break gradle caching.
   * Use {@link InstrumentationTransformerContext#setData(byte[])} for modification.
   *
   * @return the unmodified file
   */
  File getOriginalFile();

  /**
   * @return the content of the currently modified file as a byte array
   */
  byte[] getData();

  /**
   * Overrides the content of the currently modified file.
   * Data passed to this method will be used in {@link InstrumentationTask} and will override the original
   * file data in the final compilation artifact.
   * If parameter data is null, the file will be removed from compilation.
   *
   * @param data the data to set the new file data to
   */
  void setData(byte[] data);
}
