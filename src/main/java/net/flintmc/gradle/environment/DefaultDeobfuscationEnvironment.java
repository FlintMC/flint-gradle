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

package net.flintmc.gradle.environment;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.minecraft.data.environment.DefaultInput;
import net.flintmc.gradle.minecraft.data.environment.EnvironmentType;
import net.flintmc.gradle.util.Util;
import okhttp3.OkHttpClient;

public abstract class DefaultDeobfuscationEnvironment implements DeobfuscationEnvironment {

  private final DefaultInput input;
  private final String name;
  private final EnvironmentType environmentType;

  public DefaultDeobfuscationEnvironment(
      DefaultInput input, String name, EnvironmentType environmentType) {
    this.input = input;
    this.name = name;
    this.environmentType = environmentType;
  }

  @Override
  public String name() {
    return environmentType.getName();
  }

  /** {@inheritDoc} */
  @Override
  public Collection<MavenArtifact> getCompileArtifacts(MavenArtifact client, MavenArtifact server) {
    String version = getVersion(client, server);

    // We only need the client artifact for compilation, this might change at some point
    // if we support servers
    return Collections.singletonList(getJoinedArtifact(version));
  }

  /** {@inheritDoc} */
  @Override
  public Collection<MavenArtifact> getRuntimeArtifacts(MavenArtifact client, MavenArtifact server) {
    String version = getVersion(client, server);

    // The runtime always requires the joined artifact
    return Collections.singletonList(getJoinedArtifact(version));
  }

  /**
   * Retrieves the classifier for the current configuration.
   *
   * @param sources If {@code true}, the returned classifier will be a sources classifier
   * @return The generated classifier
   */
  protected String getClassifier(boolean sources) {
    return this.name
        + "-"
        + input.getConfigVersion()
        + "_"
        + input.getMappingsVersion()
        + (sources ? "-sources" : "");
  }

  /**
   * Retrieves the client artifact of the current configuration.
   *
   * @param version The version of minecraft
   * @return The client artifact with the given version
   */
  protected MavenArtifact getClientArtifact(String version) {
    return new MavenArtifact("net.minecraft", "client", version, getClassifier(false));
  }

  /**
   * Retrieves the server artifact of the current configuration.
   *
   * @param version The version of minecraft
   * @return The server artifact with the given version
   */
  private MavenArtifact getServerArtifact(String version) {
    return new MavenArtifact("net.minecraft", "server", version, getClassifier(false));
  }

  /**
   * Retrieves the joined artifact of the current configuration.
   *
   * @param version The version of minecraft
   * @return The joined artifact with the given version
   */
  protected MavenArtifact getJoinedArtifact(String version) {
    return new MavenArtifact("net.minecraft", "joined", version, getClassifier(false));
  }

  /**
   * Retrieves the version of the client and server POM.
   *
   * @param clientPom The client artifact, may be null
   * @param serverPom The server artifact, may be null
   * @return The version of the client and server POM
   * @throws IllegalArgumentException If the version of the client and server POM mismatch or both
   *     client and server POM are null
   */
  protected String getVersion(MavenArtifact clientPom, MavenArtifact serverPom) {
    String version = null;
    if (clientPom != null) {
      // The client is given, retrieve its version
      version = clientPom.getVersion();
    }

    if (serverPom != null) {
      // The server is given
      if (version != null && !serverPom.getVersion().equals(version)) {
        // Client and server version are not equal, this is not allowed
        throw new IllegalArgumentException(
            "Client and server version mismatch, client = "
                + clientPom.getVersion()
                + ", server = "
                + serverPom.getVersion());
      } else if (version == null) {
        // If the client has not been set, use the server version
        version = serverPom.getVersion();
      }
    }

    if (version == null) {
      // Received neither a server nor a client POM, this is not allowed
      throw new IllegalArgumentException("Both client and server POM are null");
    }

    return version;
  }

  /**
   * Copies resource entries into the given jar from a source jar.
   *
   * @param sourceJar The jar to use as source jar for resources
   * @param jar The target jar to copy assets into
   * @throws IOException If an I/O error occurs while copying
   */
  protected void addResources(Path sourceJar, Path jar) throws IOException {
    Set<String> existingResources = new HashSet<>();

    // Open the initial jar file to check which assets exist already
    try (JarFile jarFile = new JarFile(jar.toFile())) {
      Enumeration<JarEntry> entries = jarFile.entries();

      // Iterate all jar entries
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        if (entry.getName().startsWith("assets/")
            || entry.getName().startsWith("/data")
            || entry.getName().equals("pack.png")
            || entry.getName().equals("version.json")
            || entry.getName().equals("pack.mcmeta")) {
          // Found an assets entry, index it
          existingResources.add(entry.getName());
        }
      }
    }

    // Create a temporary jar to read from
    Path temporaryJar = Files.createTempFile(this.name + "_resource_fix", ".jar");

    try {
      // Copy the original jar to the temporary one
      Files.copy(jar, temporaryJar, StandardCopyOption.REPLACE_EXISTING);

      try (
      // Open the required streams, 2 for reading from the original and resources jar,
      // one for writing to the output
      JarInputStream originalSource = new JarInputStream(Files.newInputStream(temporaryJar));
          JarInputStream resourcesSource = new JarInputStream(Files.newInputStream(sourceJar));
          JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
        JarEntry originalEntry;

        // Copy all of the original jar entries
        while ((originalEntry = originalSource.getNextJarEntry()) != null) {
          output.putNextEntry(originalEntry);
          Util.copyStream(originalSource, output);
          output.closeEntry();

          // Avoid adding duplicates
          existingResources.add(originalEntry.getName());
        }

        JarEntry resourcesEntry;

        // Iterate all resource jar entries
        while ((resourcesEntry = resourcesSource.getNextJarEntry()) != null) {
          if ((!resourcesEntry.getName().startsWith("assets/")
                      && !resourcesEntry.getName().startsWith("data/")
                      && !resourcesEntry.getName().equals("pack.png")
                      && !resourcesEntry.getName().equals("version.json"))
                  && !resourcesEntry.getName().equals("pack.mcmeta")
              || existingResources.contains(resourcesEntry.getName())) {
            // The entry is not a resource or exists already in the other jar, skip it
            continue;
          }

          // Copy the resource entry
          output.putNextEntry(resourcesEntry);
          Util.copyStream(resourcesSource, output);
          output.closeEntry();

          // Avoid adding duplicates
          existingResources.add(resourcesEntry.getName());
        }
      }
    } finally {
      // Make sure to delete the temporary file
      Files.delete(temporaryJar);
    }
  }


  /**
   * Downloads and extracts a ZIP if the output does not exist already.
   *
   * @param cacheFileProvider The provider to use for file caching
   * @param httpClient The HTTP client to use for downloading
   * @param url The URL to retrieve the file from
   * @param outputName The name of the output file
   * @return The path to the extracted directory
   * @throws DeobfuscationException If the file fails to download or extract
   */
  protected Path downloadAndExtractZip(
      org.gradle.api.logging.Logger logger,
      EnvironmentCacheFileProvider cacheFileProvider,
      OkHttpClient httpClient,
      String url,
      String outputName)
      throws DeobfuscationException {
    // Retrieve the paths to write to
    Path outputZip = cacheFileProvider.file(outputName + ".zip");
    Path outputDir = cacheFileProvider.directory(outputName);

    if (!Files.exists(outputZip) && !Files.isDirectory(outputDir)) {
      // If both paths don't exist, download the zip
      if (httpClient == null) {
        throw new DeobfuscationException(
            "Can't download " + outputName + " when working in offline mode");
      }

      try {
        logger.lifecycle("Downloading {}", outputName);
        Util.download(httpClient, new URI(url), outputZip);
      } catch (IOException | URISyntaxException e) {
        throw new DeobfuscationException("Failed to download " + outputName, e);
      }
    }

    if (!Files.isDirectory(outputDir)) {
      // If the output path does not exist, extract the zip
      try {
        Util.extractZip(outputZip, outputDir);
      } catch (IOException e) {
        throw new DeobfuscationException("Failed to extract zip for " + outputName, e);
      }
    }

    return outputDir;
  }

}
