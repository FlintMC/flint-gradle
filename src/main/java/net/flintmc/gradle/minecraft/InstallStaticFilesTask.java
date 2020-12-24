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

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.json.JsonConverter;
import net.flintmc.gradle.manifest.dev.DevelopmentStaticFiles;
import net.flintmc.gradle.util.MaybeNull;
import net.flintmc.gradle.util.Util;
import net.flintmc.installer.impl.repository.models.PackageModel;
import net.flintmc.installer.impl.repository.models.install.DownloadFileDataModel;
import net.flintmc.installer.impl.repository.models.install.InstallInstructionModel;
import net.flintmc.installer.impl.repository.models.install.InstallInstructionTypes;
import okhttp3.OkHttpClient;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Task for downloading and installing static files.
 */
public class InstallStaticFilesTask extends DefaultTask {
  private final OkHttpClient httpClient;
  private final PotentialMinecraftClasspath potentialClasspath;
  private final String minecraftVersion;
  private final File workingDir;

  /**
   * Constructs a new {@link InstallStaticFilesTask}.
   *
   * @param httpClient         The HTTP client to use for potential downloads
   * @param potentialClasspath The potential classpath to search
   * @param minecraftVersion   The minecraft version to collect the classpath for
   * @param workingDir         The directory to use as a root directory for the install
   */
  @Inject
  public InstallStaticFilesTask(
      MaybeNull<OkHttpClient> httpClient,
      PotentialMinecraftClasspath potentialClasspath,
      String minecraftVersion,
      File workingDir
  ) {
    this.httpClient = httpClient.get();
    this.potentialClasspath = potentialClasspath;
    this.minecraftVersion = minecraftVersion;
    this.workingDir = workingDir;
  }

  private Set<File> classpath;
  private Map<File, StaticFileSource> sources;

  /**
   * Retrieves the minecraft version this task is collecting static files for.
   *
   * @return The minecraft version this task is collecting static files for
   */
  @Input
  public String getMinecraftVersion() {
    return minecraftVersion;
  }

  /**
   * Retrieves the classpath to retrieve the manifest files for packages from
   *
   * @return The classpath to retrieve the manifest files for packages from
   */
  @Classpath
  public Set<File> getClasspath() {
    if (classpath == null) {
      classpath = potentialClasspath.getRealClasspath(getProject(), minecraftVersion).getFiles();
    }

    return classpath;
  }

  /**
   * Computes the work this task has to do.
   */
  private void compute() {
    if (sources != null && classpath != null) {
      return;
    }

    // Convert the entire classpath to URLs
    Set<File> classpath = getClasspath();
    URL[] classpathAsURLs = new URL[classpath.size()];

    int i = 0;
    for (File file : classpath) {
      try {
        classpathAsURLs[i++] = file.toURI().toURL();
      } catch (MalformedURLException e) {
        throw new FlintGradleException("Failed to convert file " + file.getAbsolutePath() + " to URL", e);
      }
    }

    // Set up the class loader, it will just be used to retrieve resources
    ClassLoader loader = new URLClassLoader(classpathAsURLs);
    Set<PackageModel> manifests = new HashSet<>();

    try {
      for (URL url : new HashSet<>(Collections.list(loader.getResources("manifest.json")))) {
        try (InputStream manifestStream = Util.getURLStream(httpClient, url.toURI())) {
          // Read the manifest
          manifests.add(
              JsonConverter.PACKAGE_MODEL_SERIALIZER.fromString(Util.readAll(manifestStream), PackageModel.class));
        } catch (IOException e) {
          throw new FlintGradleException("Failed to read manifest " + url.toExternalForm(), e);
        }
      }
    } catch (IOException | URISyntaxException e) {
      throw new FlintGradleException("Failed to load manifests from classpath", e);
    }

    sources = new HashMap<>();
    // Index all manifests
    for (PackageModel manifest : manifests) {
      for (InstallInstructionModel installInstruction : manifest.getInstallInstructions()) {
        // Filter for DOWNLOAD_FILE instructions
        if (installInstruction.getType().equals(InstallInstructionTypes.DOWNLOAD_FILE.toString())) {
          // Try to retrieve a development environment override
          DownloadFileDataModel data = installInstruction.getData();
          File localFile = DevelopmentStaticFiles.getFor(
              manifest.getGroup(), manifest.getName(), manifest.getVersion(), data.getPath());

          // Compute where the file should be
          File target = new File(workingDir, data.getPath());

          if (localFile != null) {
            // There is an override available
            sources.put(target, new LocalSource(localFile));
          } else {
            // No override available, download
            sources.put(target, new RemoteSource(data));
          }
        }
      }
    }
  }

  /**
   * Performs the install of all static files.
   */
  @TaskAction
  public void performInstall() {
    compute();
    for (Map.Entry<File, StaticFileSource> entry : sources.entrySet()) {
      File target = entry.getKey();
      StaticFileSource source = entry.getValue();

      // Install the file
      try {
        source.install(target);
      } catch(IOException e) {
        throw new FlintGradleException("Failed to install static files", e);
      }
    }
  }

  /**
   * Base class for local and remote file install sources.
   */
  private interface StaticFileSource {
    /**
     * Installs the file given from this source to the given target file.
     *
     * @param target The file to install the source file to
     * @throws IOException If an I/O error occurs
     */
    void install(File target) throws IOException;

    /**
     * Retrieves the dependency used by gradle for up-to-date checks.
     *
     * @return The dependency used by gradle for up-to-date checks
     */
    @Input
    Object getDependency();
  }

  /**
   * Source for static files which will be copied.
   */
  public static class LocalSource implements StaticFileSource {
    private final File source;

    /**
     * Constructs a new {@link LocalSource} from the given file.
     *
     * @param source The file to use as a source
     */
    public LocalSource(File source) {
      this.source = source;
    }

    @Override
    public void install(File target) throws IOException {
      if (!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) {
        throw new IOException("Failed to create directory " + target.getParentFile().getAbsolutePath());
      }

      // Simply copy the file
      try (
          FileInputStream in = new FileInputStream(source);
          FileOutputStream out = new FileOutputStream(target)
      ) {
        Util.copyStream(in, out);
      }
    }

    @InputFile
    @Override
    public File getDependency() {
      return source;
    }
  }

  /**
   * Source for static files which will be downloaded from a server.
   */
  public class RemoteSource implements StaticFileSource {
    private final DownloadFileDataModel model;

    /**
     * Constructs a new {@link RemoteSource} for the given model.
     *
     * @param model The data model containing relevant information
     */
    private RemoteSource(DownloadFileDataModel model) {
      this.model = model;
    }

    @Override
    public void install(File target) throws IOException {
      String localMD5 = null;
      if (target.isFile()) {
        // File exists, check MD5
        localMD5 = Util.md5Hex(Files.readAllBytes(target.toPath()));
        if (localMD5.equals(model.getMd5())) {
          // MD5 matches, skip download
          return;
        }
      }

      if (httpClient == null) {
        String errorMessage = target.isFile() ?
            "the md5 checksums " + localMD5 + " of the static file " + model.getPath() + " (" + target.getAbsolutePath()
                + ") does not match the expected value " + model.getMd5() :
            "the static file " + model.getPath() + " (" + target.getAbsolutePath() + ") is missing locally";

        throw new FlintGradleException(
            "Gradle is operating in offline mode and can't download files, but " + errorMessage);
      }

      try {
        // MD5 mismatch or the file does not exist, download it
        Util.download(
            httpClient, URI.create(model.getUrl()), target.toPath(), getProject(), StandardCopyOption.REPLACE_EXISTING);
      } catch(IOException e) {
        throw new IOException("Failed to download file from " + model.getUrl() + " to " + target.toPath(), e);
      }
    }

    @Input
    @Override
    public URI getDependency() {
      return URI.create(model.getUrl());
    }
  }
}
