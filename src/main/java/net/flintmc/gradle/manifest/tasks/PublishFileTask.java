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

import net.flintmc.gradle.manifest.ManifestConfigurator;
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
import java.net.URI;
import java.nio.file.Files;

/**
 * Task that uploads a file to the distributor.
 */
public class PublishFileTask extends PublishTaskBase {
  @InputFile
  private File fileToUpload;

  @Input
  private URI targetURI;

  /**
   * Constructs a new {@link PublishFileTask} for the given file and URI.
   *
   * @param configurator The manifest configurator creating this task
   * @param httpClient   The HTTP client to use for uploading the file
   * @param fileToUpload The file which should be uploaded
   * @param targetURI    The URI to upload the file to
   */
  @Inject
  public PublishFileTask(
      ManifestConfigurator configurator, MaybeNull<OkHttpClient> httpClient, File fileToUpload, URI targetURI) {
    super(configurator, httpClient.get());
    this.fileToUpload = fileToUpload;
    this.targetURI = targetURI;
  }

  /**
   * Retrieves the file which should be uploaded.
   *
   * @return The file which should be uploaded
   */
  public File getFileToUpload() {
    return fileToUpload;
  }

  /**
   * Sets the file which should be uploaded.
   *
   * @param fileToUpload The file which should be uploaded
   */
  public void setFileToUpload(File fileToUpload) {
    this.fileToUpload = fileToUpload;
  }

  /**
   * Retrieves the URI to upload the file to.
   *
   * @return The URI to upload the file to
   */
  public URI getTargetURI() {
    return targetURI;
  }

  /**
   * Sets the URI the file should be uploaded to.
   *
   * @param targetURI The URI the file should be uploaded to
   */
  public void setTargetURI(URI targetURI) {
    this.targetURI = targetURI;
  }

  /**
   * Publishes the configured file.
   */
  @TaskAction
  public void publish() {
    // Create the entity to upload

    MediaType mediaType;
    try {
      // Set the content type if it can be determined
      mediaType = MediaType.get(Files.probeContentType(fileToUpload.toPath()));
    } catch (IOException e) {
      // Not too bad, but still something we should log
      getLogger().warn("Failed to determine content type of file " + fileToUpload.getAbsolutePath(), e);
      mediaType = MediaType.get("application/octet-stream");
    }

    publish(targetURI, RequestBody.create(fileToUpload, mediaType));
  }
}
