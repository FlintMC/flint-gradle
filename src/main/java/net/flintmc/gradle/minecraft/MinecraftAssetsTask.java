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

package net.flintmc.gradle.minecraft;

import com.fasterxml.jackson.databind.JsonNode;
import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.json.JsonConverter;
import net.flintmc.gradle.minecraft.data.version.AssetIndex;
import net.flintmc.gradle.minecraft.data.version.VersionManifest;
import net.flintmc.gradle.util.Util;
import okhttp3.OkHttpClient;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Task for downloading and install minecraft assets.
 */
@CacheableTask
public class MinecraftAssetsTask extends DefaultTask {
  private static final URI ASSET_BASE_URL = URI.create("https://resources.download.minecraft.net");

  private VersionManifest manifest;
  private Path directory;

  /**
   * Sets the version manifest to retrieve the assets index from.
   *
   * @param manifest The manifest to retrieve the assets index from
   */
  public void setVersionManifest(VersionManifest manifest) {
    this.manifest = manifest;
  }

  /**
   * Sets the output directory to write assets into.
   *
   * @param directory The directory to write assets into
   */
  public void setOutputDirectory(Path directory) {
    this.directory = directory;
  }

  /**
   * Downloads all assets for the configured minecraft manifest.
   *
   * @throws IOException If an I/O error occurs while downloading
   */
  @TaskAction
  public void run() throws IOException {
    // Validate state
    if (manifest == null) {
      throw new IllegalStateException("The manifest has not been configured");
    } else if (directory == null) {
      throw new IllegalStateException("The output directory has not been configured");
    }

    // Retrieve the json index declaration
    AssetIndex index = manifest.getAssetIndex();

    // Calculate the target path of the index json
    Path indexFile = directory.resolve("indexes").resolve(index.getId() + ".json");
    OkHttpClient httpClient = getProject().getPlugins().getPlugin(FlintGradlePlugin.class).getHttpClient();

    if (!Files.exists(indexFile)) {
      // The asset index file has not been downloaded yet, do so now
      getLogger().lifecycle("Downloading assets index for " + index.getId());
      Util.download(httpClient, index.getUrl(), indexFile);
    }

    // Parse the index file
    JsonNode root;
    try (InputStream stream = Files.newInputStream(indexFile)) {
      root = JsonConverter.OBJECT_MAPPER.readTree(stream);
    }

    // Resolve the objects dir within the asset store
    Path objectsDir = directory.resolve("objects");

    // Get the object describing all assets
    JsonNode objects = root.get("objects").requireNonNull();
    Iterator<String> it = objects.fieldNames();

    // Iterate over every asset object
    while (it.hasNext()) {
      // Extract the required properties
      String objectName = it.next();
      String hash = objects.get(objectName).get("hash").requireNonNull().asText();

      // Calculate the download and store path
      String assetPath = hash.substring(0, 2) + "/" + hash;

      Path assetTargetPath = objectsDir.resolve(assetPath);
      if (!Files.isRegularFile(assetTargetPath)) {
        // The asset does not exist yet, download it
        getLogger().lifecycle("Downloading asset {} ({})", objectName, assetPath);
        Util.download(httpClient, Util.concatURI(ASSET_BASE_URL, assetPath), assetTargetPath);
      }
    }
  }
}
