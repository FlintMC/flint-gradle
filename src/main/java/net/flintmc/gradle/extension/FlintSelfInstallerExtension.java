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

import groovy.lang.Closure;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import javax.annotation.Nonnull;

/**
 * Extension configuring the self installer.
 */
public class FlintSelfInstallerExtension implements Configurable<FlintSelfInstallerExtension> {
  /**
   * Default self installer dependency notation. Defaults to the in house provided installer solution.
   */
  public static final String DEFAULT_SELF_INSTALLER_DEPENDENCY = "net.flintmc.installer:frontend-gui:1.1.12";

  /**
   * Default self installer dependency main class. Defaults to the main class of the in house provided solution.
   */
  public static final String DEFAULT_SELF_INSTALLER_MAIN_CLASS = "net.flintmc.installer.frontend.gui.SelfInstaller";

  private boolean enabled;
  private Object dependencyNotation;
  private String mainClass;

  /**
   * Constructs a new {@link FlintSelfInstallerExtension} and initializes it with the defaults.
   */
  public FlintSelfInstallerExtension() {
    this.enabled = true;
    this.dependencyNotation = DEFAULT_SELF_INSTALLER_DEPENDENCY;
    this.mainClass = DEFAULT_SELF_INSTALLER_MAIN_CLASS;
  }

  /**
   * Constructs a new {@link FlintSelfInstallerExtension} copying from the given parent.
   *
   * @param parent The parent to copy from
   */
  public FlintSelfInstallerExtension(FlintSelfInstallerExtension parent) {
    this.enabled = parent.enabled;
    this.dependencyNotation = parent.dependencyNotation;
    this.mainClass = parent.mainClass;
  }

  /**
   * Determines whether the self installer is enabled.
   *
   * @return {@code true} if the self installer generation is enabled, {@code false} otherwise
   */
  public boolean isEnabled() {
    return enabled;
  }

  /**
   * Enables or disables the generation of the flint self installer binaries.
   *
   * @param enabled If {@code true}, the plugin will provide tasks to generate self installer jars
   */
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  /**
   * Enables or disables the generation of the flint self installer binaries.
   *
   * @param enabled If {@code true}, the plugin will provide tasks to generate self installer jars
   */
  public void enable(boolean enabled) {
    setEnabled(enabled);
  }

  /**
   * Retrieves the flint self installer dependency notation.
   *
   * @return The flint self installer dependency notation
   */
  public Object getDependencyNotation() {
    return dependencyNotation;
  }

  /**
   * Overwrites the flint self installer dependency notation.
   *
   * @param dependencyNotation The new flint self installer dependency notation
   */
  public void setDependencyNotation(Object dependencyNotation) {
    this.dependencyNotation = dependencyNotation;
  }

  /**
   * Overwrites the flint self installer dependency notation.
   *
   * @param dependencyNotation The new flint self installer dependency notation
   */
  public void dependencyNotation(Object dependencyNotation) {
    setDependencyNotation(dependencyNotation);
  }


  /**
   * Retrieves the flint self installer main class.
   *
   * @return The flint self installer main class
   */
  public String getMainClass() {
    return mainClass;
  }

  /**
   * Overwrites the flint self installer main class.
   *
   * @param mainClass The new flint self installer main class
   */
  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  /**
   * Overwrites the flint self installer main class.
   *
   * @param mainClass The new flint self installer main class
   */
  public void mainClass(String mainClass) {
    setMainClass(mainClass);
  }

  @Override
  @Nonnull
  public FlintSelfInstallerExtension configure(@Nonnull Closure cl) {
    return ConfigureUtil.configureSelf(cl, this);
  }
}
