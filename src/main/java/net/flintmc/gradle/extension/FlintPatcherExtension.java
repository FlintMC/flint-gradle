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

package net.flintmc.gradle.extension;

import java.io.File;
import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.patcher.GeneratePatchesTask;
import net.flintmc.gradle.util.Util;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;

/** This extensions adds a patcher that allows us to modify the minecraft source code. */
public class FlintPatcherExtension {

  public static final String NAME = "patcher";

  private boolean enabled = false;

  private File cleanSource;
  private File modifiedSource;
  private File patches;

  private String originalPrefix = "a/";
  private String modifiedPrefix = "b/";

  public FlintPatcherExtension(FlintGradlePlugin plugin) {
    TaskContainer taskContainer = plugin.getProject().getTasks();

    TaskProvider<GeneratePatchesTask> generatePatchesProvider =
        taskContainer.register(GeneratePatchesTask.NAME, GeneratePatchesTask.class);

    generatePatchesProvider.configure(
        task -> {
          task.setGroup(NAME);
          task.setCleanSource(this.getCleanSource());
          task.setModifiedSource(this.getModifiedSource());
          task.setPatches(this.getPatches());
          task.setOriginalPrefix(this.getOriginalPrefix());
          task.setModifiedPrefix(this.getModifiedPrefix());
        });
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    HackyPatcherData.setEnabled(enabled);
  }

  public boolean isEnabled() {
    return enabled;
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
   * Changes the location where the clean source code is located.
   *
   * @param cleanSource The new location of the clean source code.
   */
  public void setCleanSource(File cleanSource) {
    this.cleanSource = cleanSource;
    HackyPatcherData.setCleanSource(cleanSource);
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
   * Changes the location where the modified source code is located.
   *
   * @param modifiedSource The new location of the modified source code.
   */
  public void setModifiedSource(File modifiedSource) {
    this.modifiedSource = modifiedSource;
    HackyPatcherData.setModifiedSource(modifiedSource);
  }

  /**
   * Retrieves the location where the patches should be stored.
   *
   * @return The location where the patches should be stored.
   */
  public File getPatches() {
    return patches;
  }

  /**
   * Changes the location where the patches should be stored.
   *
   * @param patches The new location where the patches should be stored.
   */
  public void setPatches(File patches) {
    this.patches = patches;
    HackyPatcherData.setPatches(patches);
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
   * Changes the original prefix.
   *
   * @param originalPrefix The new original prefix.
   */
  public void setOriginalPrefix(String originalPrefix) {
    this.originalPrefix = originalPrefix;
    HackyPatcherData.setOriginalPrefix(originalPrefix);
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
   * Changes the modified prefix.
   *
   * @param modifiedPrefix The new modified prefix.
   */
  public void setModifiedPrefix(String modifiedPrefix) {
    this.modifiedPrefix = modifiedPrefix;
    HackyPatcherData.setModifiedPrefix(modifiedPrefix);
  }

  public static class HackyPatcherData  {

    private static boolean enabled;

    private static File cleanSource;
    private static File modifiedSource;
    private static File patches;

    private static String originalPrefix;
    private static String modifiedPrefix;

    public static boolean isEnabled() {
      return enabled;
    }

    public static void setEnabled(boolean enabled) {
      HackyPatcherData.enabled = enabled;
    }

    public static File getCleanSource() {
      return cleanSource;
    }

    public static void setCleanSource(File cleanSource) {
      HackyPatcherData.cleanSource = cleanSource;
    }

    public static File getModifiedSource() {
      return modifiedSource;
    }

    public static void setModifiedSource(File modifiedSource) {
      HackyPatcherData.modifiedSource = modifiedSource;
    }

    public static File getPatches() {
      return patches;
    }

    public static void setPatches(File patches) {
      HackyPatcherData.patches = patches;
    }

    public static String getOriginalPrefix() {
      return originalPrefix;
    }

    public static void setOriginalPrefix(String originalPrefix) {
      HackyPatcherData.originalPrefix = originalPrefix;
    }

    public static String getModifiedPrefix() {
      return modifiedPrefix;
    }

    public static void setModifiedPrefix(String modifiedPrefix) {
      HackyPatcherData.modifiedPrefix = modifiedPrefix;
    }
  }
}
