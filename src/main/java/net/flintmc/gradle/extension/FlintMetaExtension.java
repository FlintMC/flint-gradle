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
import org.gradle.api.Project;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import javax.annotation.Nonnull;

/**
 * Extension for configuring package metadata properties.
 */
public class FlintMetaExtension implements Configurable<FlintMetaExtension> {
  private boolean inheritParent;
  private boolean isMetaPackage;
  private String parent;

  /**
   * Constructs a {@link FlintMetaExtension} with default values.
   */
  public FlintMetaExtension() {
    this.inheritParent = true;
    this.isMetaPackage = false;
    this.parent = null;
  }

  /**
   * Constructs a {@link FlintMetaExtension} by partially copying the parent values.
   *
   * @param parent The parent to partially copy from
   */
  public FlintMetaExtension(FlintMetaExtension parent) {
    this.inheritParent = parent.inheritParent;
    this.isMetaPackage = false;
    this.parent = parent.parent;
  }

  /**
   * Sets whether the parent should be inherited from the parent project. This setting only has an effect
   * if {@link #getParent()} is {@code null}. Note that this does inherit the parent in the following way:
   * <ol>
   *   <li>If this project has no parent, no parent is set</li>
   *   <li>If this projects parent has no parent, this projects parent becomes the parent</li>
   *   <li>If this projects parent has a parent, the parent of this project becomes the parent</li>
   * </ol>
   *
   * @param inheritParent Whether the parent should be inherited from the parent project
   */
  public void inheritParent(boolean inheritParent)  {
    this.inheritParent = inheritParent;
  }

  /**
   * Determines whether the parent should be inherited.
   *
   * @return {@code true} if the parent should be inherited, {@code false} otherwise
   */
  public boolean shouldInheritParent() {
    return this.inheritParent;
  }

  /**
   * Sets whether this package should be considered a meta package.
   * Meta packages should be used if there is no code associated with the package itself and instead it is used
   * as a parent for other packages only.
   *
   * @param isMetaPackage If {@code true}, this package is a meta package
   */
  public void metaPackage(boolean isMetaPackage) {
    this.isMetaPackage = isMetaPackage;
  }

  /**
   * Determines Sets whether this package should be considered a meta package.
   *
   * @return {@code true} if this package should be considered a meta package, {@code false} otherwise
   */
  public boolean isMetaPackage() {
    return this.isMetaPackage;
  }

  /**
   * Sets the parent package of this package based on its name.
   * A parent package is used to group packages together. Typically parent packages are meta packages, although
   * they don't have to be.
   *
   * @param parent The new parent of this package
   */
  public void parent(String parent) {
    this.parent = parent;
  }

  /**
   * Sets the parent package of this package based on a project.
   * A parent package is used to group packages together. Typically parent packages are meta packages, although
   * they don't have to be.
   *
   * @param parent The new parent of this package
   */
  public void parent(Project parent) {
    this.parent = parent.getName();
  }

  /**
   * Retrieves the name of the parent package of this package.
   *
   * @return The name of the parent package, or {@code null}, if none
   */
  public String getParent() {
    return this.parent;
  }

  /**
   * Configures the values of this instance with the given closure.
   *
   * @param closure The closure to pass this instance to
   * @return Configured this
   */
  @Nonnull
  @Override
  public FlintMetaExtension configure(@Nonnull Closure closure) {
    return ConfigureUtil.configureSelf(closure, this);
  }
}
