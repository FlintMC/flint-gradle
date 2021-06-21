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

import net.flintmc.gradle.java.instrumentation.api.context.InstrumentationClassTransformerContext;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.SourceSet;

import java.io.File;

public class DefaultInstrumentationClassTransformerContext extends DefaultInstrumentationRawTransformerContext implements InstrumentationClassTransformerContext {

  private final String className;
  private final String packageName;
  private final File originalClassFile;
  private byte[] classData;

  public DefaultInstrumentationClassTransformerContext(
      SourceSet sourceSet,
      FileCollection originalClassDirectories,
      File originalResourceDirectory,
      String className,
      String packageName,
      File originalClassFile,
      byte[] classData) {
    super(sourceSet, originalClassDirectories, originalResourceDirectory);
    this.className = className;
    this.packageName = packageName;
    this.originalClassFile = originalClassFile;
    this.classData = classData;
  }

  @Override
  public String getClassName() {
    return this.className;
  }

  @Override
  public String getPackageName() {
    return this.packageName;
  }

  @Override
  public File getOriginalClassFile() {
    return this.originalClassFile;
  }

  @Override
  public byte[] getClassData() {
    return this.classData;
  }

  @Override
  public void setClassData(byte[] classData) {
    this.classData = classData;
  }

}
