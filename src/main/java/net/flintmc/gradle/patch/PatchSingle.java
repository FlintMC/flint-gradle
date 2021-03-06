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

import com.cloudbees.diff.Hunk;
import java.io.File;
import net.flintmc.gradle.patch.state.PatchMode;

public class PatchSingle {

  private String targetIndex;
  private String targetPath;
  private Hunk[] hunks;
  private File targetFile;
  private boolean noEndingNewline;
  private boolean binary;

  private PatchMode patchMode;

  /**
   * Retrieves the target index of this patch.
   *
   * @return The patch's target index.
   */
  public String getTargetIndex() {
    return targetIndex;
  }

  /**
   * Changes the target index of this patch.
   *
   * @param targetIndex The new target index.
   */
  public void setTargetIndex(String targetIndex) {
    this.targetIndex = targetIndex;
  }

  /**
   * Retrieves the path of the target of this patch.
   *
   * @return The path of the target of this patch.
   */
  public String getTargetPath() {
    return targetPath;
  }

  /**
   * Changes the path of the target of this patch.
   *
   * @param targetPath The new target path.
   */
  public void setTargetPath(String targetPath) {
    this.targetPath = targetPath;
  }

  /**
   * Retrieves an array of hunks of this patch.
   *
   * @return An array of hunks of this patch.
   */
  public Hunk[] getHunks() {
    return hunks;
  }

  /**
   * Sets a new array of hunks for this patch.
   *
   * @param hunks The new array.
   */
  public void setHunks(Hunk[] hunks) {
    this.hunks = hunks;
  }

  /**
   * Retrieves the target on which the patch should be applied.
   *
   * @return The target on which the patch should be applied.
   */
  public File getTargetFile() {
    return targetFile;
  }

  /**
   * Changes the target on which the patch should be applied.
   *
   * @param targetFile The new target file.
   */
  public void setTargetFile(File targetFile) {
    this.targetFile = targetFile;
  }

  /**
   * Whether the patch has no ending new line.
   *
   * @return {@code true} if the patch has no ending new line, otherwise {@code false}.
   */
  public boolean isNoEndingNewline() {
    return noEndingNewline;
  }

  /**
   * Changes whether the patch has no ending new line.
   *
   * @param noEndingNewline {@code true} if the patch has no ending new line, otherwise {@code
   *     false}.
   */
  public void setNoEndingNewline(boolean noEndingNewline) {
    this.noEndingNewline = noEndingNewline;
  }

  /**
   * Whether the patch is binary.
   *
   * @return {@code true} if the patch is binary, otherwise {@code false}.
   */
  public boolean isBinary() {
    return binary;
  }

  /**
   * Changes whether the patch is binary.
   *
   * @param binary {@code true} if the patch is binary, otherwise {@code false}.
   */
  public void setBinary(boolean binary) {
    this.binary = binary;
  }

  /**
   * Retrieves the mode of this patch.
   *
   * @return The patch's mode.
   */
  public PatchMode getPatchMode() {
    return patchMode;
  }

  /**
   * Changes the mode of this patch.
   *
   * @param mode The new patch mode.
   */
  public void setPatchMode(PatchMode mode) {
    this.patchMode = mode;
  }
}
