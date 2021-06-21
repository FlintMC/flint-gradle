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

package net.flintmc.gradle.minecraft.data.version;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.flintmc.gradle.minecraft.data.manifest.MinecraftVersionType;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VersionManifest {
  private String id;
  private VersionedArguments arguments;
  @JsonProperty("assets")
  private String assetsVersion;
  private AssetIndex assetIndex;
  private String mainClass;
  private int minimumLauncherVersion;
  private Date releaseTime;
  private Date time;
  private MinecraftVersionType type;
  private Map<String, VersionedDownload> downloads;
  private List<VersionedLibrary> libraries;
  private Map<String, VersionedLoggingConfiguration> logging;
  private String minecraftArguments;

  public String getId() {
    return id;
  }

  public VersionedArguments getArguments() {
    return arguments;
  }

  public String getAssetsVersion() {
    return assetsVersion;
  }

  public AssetIndex getAssetIndex() {
    return assetIndex;
  }

  public String getMainClass() {
    return mainClass;
  }

  public int getMinimumLauncherVersion() {
    return minimumLauncherVersion;
  }

  public Date getReleaseTime() {
    return releaseTime;
  }

  public Date getTime() {
    return time;
  }

  public MinecraftVersionType getType() {
    return type;
  }

  public Map<String, VersionedDownload> getDownloads() {
    return downloads;
  }

  public List<VersionedLibrary> getLibraries() {
    return libraries;
  }

  public Map<String, VersionedLoggingConfiguration> getLogging() {
    return logging;
  }

  public String getMinecraftArguments() {
    return minecraftArguments;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VersionManifest that = (VersionManifest) o;
    return minimumLauncherVersion == that.minimumLauncherVersion &&
        Objects.equals(id, that.id) &&
        Objects.equals(arguments, that.arguments) &&
        Objects.equals(assetsVersion, that.assetsVersion) &&
        Objects.equals(assetIndex, that.assetIndex) &&
        Objects.equals(mainClass, that.mainClass) &&
        Objects.equals(releaseTime, that.releaseTime) &&
        Objects.equals(time, that.time) &&
        type == that.type &&
        Objects.equals(downloads, that.downloads) &&
        Objects.equals(libraries, that.libraries) &&
        Objects.equals(logging, that.logging);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, arguments, assetsVersion, assetIndex, mainClass, minimumLauncherVersion,
        releaseTime, time, type, downloads, libraries, logging);
  }
}
