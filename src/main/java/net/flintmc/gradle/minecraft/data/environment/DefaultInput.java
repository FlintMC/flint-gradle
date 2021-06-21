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

package net.flintmc.gradle.minecraft.data.environment;

import java.net.URL;

public abstract class DefaultInput {

  private String configVersion;
  private URL configDownload;
  private String mappingsVersion;
  private URL mappingsDownload;

  /**
   * Retrieves the config version of this input.
   *
   * @return The config version of this input.
   */
  public String getConfigVersion() {
    return this.configVersion;
  }

  /**
   * Retrieves the download url where the config is located.
   *
   * @return The download url where the config is located.
   */
  public URL getConfigDownload() {
    return this.configDownload;
  }

  /**
   * Retrieves the mappings version of this input.
   *
   * @return The mappings version of this input.
   */
  public String getMappingsVersion() {
    return this.mappingsVersion;
  }

  /**
   * Retrieves the download url where the mappings are located.
   *
   * @return The download url where the mappings are located.
   */
  public URL getMappingsDownload() {
    return this.mappingsDownload;
  }
}
