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

package net.flintmc.gradle.manifest.tasks;

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.manifest.ManifestConfigurator;
import net.flintmc.gradle.manifest.cache.StaticFileChecksums;
import net.flintmc.gradle.manifest.data.ManifestStaticFile;
import net.flintmc.gradle.manifest.data.ManifestStaticFileInput;
import net.flintmc.gradle.util.MaybeNull;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collection;
import java.util.Map;

public class PublishStaticFilesTask extends PublishTaskBase {
  private final ManifestStaticFileInput staticFiles;

  @InputFile
  private final File staticFilesChecksumsCacheFile;

  /**
   * Constructs a new {@link PublishStaticFilesTask} for publishing static files to the distributor.
   *
   * @param configurator                  The manifest configurator creating this task
   * @param httpClient                    The HTTP client to use for uploading the file
   * @param staticFiles                   The static files to upload
   * @param staticFilesChecksumsCacheFile The file to load the cached checksums from
   */
  @Inject
  public PublishStaticFilesTask(
      ManifestConfigurator configurator,
      MaybeNull<OkHttpClient> httpClient,
      ManifestStaticFileInput staticFiles,
      File staticFilesChecksumsCacheFile
  ) {
    super(configurator, httpClient.get());
    this.staticFiles = staticFiles;
    this.staticFilesChecksumsCacheFile = staticFilesChecksumsCacheFile;
  }

  /**
   * Retrieves the static files for this manifest.
   *
   * @return The static files for this manifest
   */
  // NOTE: This method is only required for gradle to correctly calculate the up-to-date state of this task
  @Input
  public Collection<ManifestStaticFile> getStaticFiles() {
    staticFiles.compute(getProject());
    return staticFiles.getLocalFiles().values();
  }

  /**
   * Retrieves the input file this task reads the static file checksums from.
   *
   * @return The file this task reads the static file checksums from
   */
  public File getStaticFilesChecksumsCacheFile() {
    return staticFilesChecksumsCacheFile;
  }

  /**
   * Publishes the static files.
   */
  @TaskAction
  public void publish() {
    if(!getStaticFilesChecksumsCacheFile().isFile()) {
      throw new IllegalStateException("Missing static file checksum cache file");
    }

    staticFiles.compute(getProject());

    // Load cached checksums
    StaticFileChecksums checksums;
    try {
      checksums = StaticFileChecksums.load(staticFilesChecksumsCacheFile);
    } catch(IOException e) {
      throw new FlintGradleException("IOException while loading cached static files checksums", e);
    }

    // Publish all files
    for(Map.Entry<File, ManifestStaticFile> entry : staticFiles.getLocalFiles().entrySet()) {
      File file = entry.getKey();
      ManifestStaticFile description = entry.getValue();

      if(!checksums.has(file)) {
        // Should not be possible unless the user manually excluded the checksum task
        throw new FlintGradleException("Missing checksum for file " + file.getAbsolutePath());
      }

      URI publishURI = description.getURI();

      // Publish file
      publish(publishURI, RequestBody.create(file, MediaType.get("application/octet-stream")));

      // Add .md5 to the end of the URI
      URI md5URI = publishURI.resolve(publishURI.getPath() + ".md5");
      // Upload the hash
      publish(md5URI, RequestBody.create(checksums.get(file), MediaType.get("application/octet-stream")));
    }
  }
}
