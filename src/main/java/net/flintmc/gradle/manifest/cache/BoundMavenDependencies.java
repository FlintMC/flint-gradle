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

package net.flintmc.gradle.manifest.cache;

import net.flintmc.gradle.manifest.data.ManifestMavenDependency;
import net.flintmc.gradle.util.Util;
import org.gradle.api.Project;

import java.io.*;
import java.net.URI;
import java.util.Map;

public class BoundMavenDependencies {
  /**
   * Loads the bound dependencies from a file.
   *
   * @param file The file to load the bound dependencies from
   * @return The loaded bound dependencies
   * @throws IOException If an I/O error occurs
   */
  public static Map<ManifestMavenDependency, URI> load(File file) throws IOException {
    try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
      return Util.forceCast(in.readObject());
    } catch(ClassNotFoundException e) {
      throw new IOException("Failed to load cached bound maven dependencies due to ClassNotFoundException", e);
    }
  }

  /**
   * Saves the bound dependencies to a file.
   *
   * @param file         The file to save the bound dependencies to
   * @param dependencies The bound dependencies to save
   * @throws IOException If an I/O error occurs
   */
  public static void save(File file, Map<ManifestMavenDependency, URI> dependencies) throws IOException {
    try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
      out.writeObject(dependencies);
    }
  }

  /**
   * Retrieves a project unique file to cache bound dependencies in.
   *
   * @param project The project to retrieve the cache file for
   * @return The cache file
   */
  public static File getCacheFile(Project project) {
    return new File(Util.getProjectCacheDir(project), "artifact-repositories.bin");
  }
}
