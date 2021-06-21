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

package net.flintmc.gradle.java.instrumentation.impl.context;

import net.flintmc.gradle.java.instrumentation.api.context.InstrumentationRawTransformerContext;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;

import java.io.File;

public class DefaultInstrumentationRawTransformerContext implements InstrumentationRawTransformerContext {

  private final SourceSet sourceSet;
  private final FileCollection originalClassDirectories;
  private final File originalResourceDirectory;

  public DefaultInstrumentationRawTransformerContext(
      SourceSet sourceSet,
      FileCollection originalClassDirectories,
      File originalResourceDirectory) {
    this.sourceSet = sourceSet;
    this.originalClassDirectories = originalClassDirectories;
    this.originalResourceDirectory = originalResourceDirectory;
  }

  @Override
  public SourceSet getSourceSet() {
    return null;
  }

  @Override
  public FileCollection getOriginalClassDirectories() {
    return null;
  }

  @Override
  public File getOriginalResourceDirectory() {
    return null;
  }
}
