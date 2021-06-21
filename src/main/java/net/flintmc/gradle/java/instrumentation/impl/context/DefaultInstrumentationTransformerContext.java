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

import net.flintmc.gradle.java.instrumentation.api.context.InstrumentationTransformerContext;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;

import java.io.File;

public class DefaultInstrumentationTransformerContext implements InstrumentationTransformerContext {


  private final SourceSet sourceSet;
  private final FileCollection originalClassDirectories;
  private final File originalResourceDirectory;
  private final File instrumentedClassDirectory;
  private final File originalFile;
  private byte[] classData;

  public DefaultInstrumentationTransformerContext(
      SourceSet sourceSet,
      FileCollection originalClassDirectories,
      File originalResourceDirectory,
      File instrumentedClassDirectory,
      File originalClassFile,
      byte[] classData
  ) {
    this.sourceSet = sourceSet;
    this.originalClassDirectories = originalClassDirectories;
    this.originalResourceDirectory = originalResourceDirectory;
    this.instrumentedClassDirectory = instrumentedClassDirectory;
    this.originalFile = originalClassFile;
    this.classData = classData;
  }

  @Override
  public SourceSet getSourceSet() {
    return this.sourceSet;
  }

  @Override
  public FileCollection getOriginalClassDirectories() {
    return this.originalClassDirectories;
  }

  @Override
  public File getOriginalResourceDirectory() {
    return this.originalResourceDirectory;
  }

  @Override
  public File getInstrumentedClassDirectory() {
    return this.instrumentedClassDirectory;
  }

  @Override
  public File getOriginalFile() {
    return this.originalFile;
  }

  @Override
  public byte[] getData() {
    return this.classData;
  }

  @Override
  public void setData(byte[] classData) {
    this.classData = classData;
  }
}
