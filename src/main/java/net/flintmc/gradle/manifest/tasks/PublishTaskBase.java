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
import net.flintmc.gradle.util.Util;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.gradle.api.DefaultTask;
import org.gradle.api.credentials.HttpHeaderCredentials;

import java.io.IOException;
import java.net.URI;

/**
 * Base for all publish tasks.
 */
public abstract class PublishTaskBase extends DefaultTask {
  private final ManifestConfigurator configurator;
  private final OkHttpClient httpClient;

  /**
   * Constructs a new {@link PublishTaskBase}.
   *
   * @param configurator The manifest configurator creating this task
   * @param httpClient   The HTTP client to use for uploading
   */
  public PublishTaskBase(ManifestConfigurator configurator, OkHttpClient httpClient) {
    this.configurator = configurator;
    this.httpClient = httpClient;
  }

  /**
   * Publishes the given entity to the given URI.
   *
   * @param uri         The URI to publish to
   * @param requestBody The HTTP request body to publish
   */
  protected final void publish(URI uri, RequestBody requestBody) {
    if (httpClient == null) {
      throw new FlintGradleException("Tried to publish file while gradle was in offline mode");
    }

    // Configure the upload
    Request.Builder put = new Request.Builder()
        .url(uri.toString())
        .put(requestBody);


    HttpHeaderCredentials credentials = Util.getDistributorCredentials(
        getProject(),
        true,
        "Set enablePublishing to false in the flint extension");

    // Add the credentials header
    put.header(credentials.getName(), credentials.getValue());

    try (Response response = httpClient.newCall(put.build()).execute()) {
      // Upload now...

      // Check the status of the upload
      int code = response.code();

      if (code < 200 || code >= 300) {
        // Unexpected response
        throw new IOException("Server responded with " + code + " (" + response.message() + ")");
      }
    } catch (IOException e) {
      throw new FlintGradleException("Failed to publish file", e);
    }
  }
}
