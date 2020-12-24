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

package net.flintmc.gradle.io;

import net.flintmc.gradle.util.Util;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.gradle.internal.impldep.org.bouncycastle.cert.ocsp.Req;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Utility class for a file with a timestamp tracking the last update time.
 */
public class TimeStampedFile {
  private static final Logger LOGGER = LoggerFactory.getLogger(TimeStampedFile.class);
  private final Path filePath;
  private final Path stampPath;

  /**
   * Constructs a new {@link TimeStampedFile} with the given path. The stamp file
   * will be created in the same directory and with the name of the original file
   * appended with {@code .stamp}.
   *
   * @param filePath The path of the real file
   */
  public TimeStampedFile(Path filePath) {
    this.filePath = filePath;
    this.stampPath = filePath.resolveSibling(filePath.getFileName() + ".stamp");
  }

  /**
   * Fetches the file from the remote backend if the local file is out of date.
   *
   * @param httpClient        The client to use for fetching the file
   * @param updateUrl         The URL to fetch the file from
   * @param dateTimeFormatter The date time formatter to use for parsing the Last-Modified Header
   * @throws IOException If an I/O error occurs while updating the file
   */
  public void update(
      OkHttpClient httpClient, String updateUrl, DateTimeFormatter dateTimeFormatter) throws IOException {
    Date lastModifiedDate = null;


    // Send a HEAD request to the Mojang server in order to retrieve the Last-Modified header
    Response headResponse = httpClient.newCall(new Request.Builder()
        .url(updateUrl)
        .head()
        .build())
        .execute();


    if (headResponse.code() != 200) {
      // Bail out of the server did not respond with 200-Ok
      throw new IOException("Server responded with status "
          + headResponse.code() + " (" + headResponse.message() + ")");
    }


    String lastModifiedHeader = headResponse.header("Last-Modified");
    if (lastModifiedHeader != null && lastModifiedHeader.length() > 0) {
      // We have received values in the Last-Modified header
      if (lastModifiedHeader != null) {
        // Reconstruct the local and remote date
        lastModifiedDate = Date.from(
            dateTimeFormatter.parse(lastModifiedHeader, ZonedDateTime::from).toInstant());
      } else {
        LOGGER.warn("Server sent an empty Last-Modified header, assuming " + filePath + " is out of date!");
      }
    } else {
      LOGGER.warn("Server did not send an Last-Modified header, assuming " + filePath + " is out of date!");
    }

    // Check if both the manifest file and the manifest time stamp exist
    if (Files.isRegularFile(this.filePath) && Files.isRegularFile(this.stampPath)
        && lastModifiedDate != null) {
      // Parse the local value from the stamp file
      long timestamp = Util.longFromByteArray(Files.readAllBytes(this.stampPath));

      Date lastLocalModifiedDate = Date.from(Instant.ofEpochMilli(timestamp));
      if (!lastLocalModifiedDate.before(lastModifiedDate)) {
        // Local file is newer
        return;
      }
    }

    // Fetch the file data
    try (Response response = httpClient.newCall(new Request.Builder()
        .url(updateUrl)
        .get()
        .build())
        .execute()) {

      if (response.code() != 200) {
        // Bail out if the server did not respond with 200-Ok
        throw new IOException("Server responded with status code "
            + response.code() + " (" + response.message() + ")");
      }

      Path parentPath = this.filePath.getParent();
      if (!Files.isDirectory(parentPath)) {
        Files.createDirectories(parentPath);
      }

      if (lastModifiedDate != null) {
        // If the server sent a last modified date, save it
        Files.write(this.stampPath, Util.longToByteArray(lastModifiedDate.getTime()));
      } else {
        // We don't know when the file is from, delete the stamp file
        Files.deleteIfExists(this.stampPath);
      }

      // Write the received data
      Files.copy(response.body().byteStream(), this.filePath, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Returns the NIO {@link Path} representation of this file.
   *
   * @return The path representation of this file
   */
  public Path toPath() {
    return this.filePath;
  }

  /**
   * Returns the IO {@link File} representation of this file.
   *
   * @return The java file representation of this file
   */
  public File toFile() {
    return this.filePath.toFile();
  }
}
