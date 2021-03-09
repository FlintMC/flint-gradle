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

package net.flintmc.gradle.util.resource;

import java.io.File;
import java.util.Collection;

/**
 * Helper class to load resources from a classpath without using a class loader.
 */
public class ResourceLoader {
  private final Collection<File> files;

  /**
   * Constructs a new resource loader.
   *
   * @param files The files to treat as classpath
   */
  public ResourceLoader(Collection<File> files) {
    this.files = files;
  }

  /**
   * Finds all resource with a certain name.
   *
   * @param resourceName The name of the resource to find in the class path
   * @return A searcher searching the classpath for the resource
   */
  public ResourceFinder findAll(String resourceName) {
    return new ResourceFinder(resourceName, files);
  }
}
