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

package net.flintmc.gradle.manifest.data;

import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.extension.FlintStaticFileDescription;
import net.flintmc.gradle.manifest.ManifestConfigurator;
import net.flintmc.gradle.util.Util;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.tasks.Internal;

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Cacheable listing of static file entries.
 */
public class ManifestStaticFileInput {
  @Internal
  private final Set<ManifestStaticFile> remoteFiles;

  @Internal
  private final Map<File, ManifestStaticFile> localFiles;

  private final ManifestConfigurator configurator;

  private boolean computed;

  /**
   * Creates a new static file input, but does not compute it.
   *
   * @param configurator The configurator currently computing the inputs
   */
  public ManifestStaticFileInput(ManifestConfigurator configurator) {
    remoteFiles = new HashSet<>();
    this.localFiles = new HashMap<>();
    this.configurator = configurator;
  }

  /**
   * Computes the static files for the manifest input.
   *
   * @param project The project to compute the files for
   */
  public void compute(Project project) {
    if(computed) {
      return;
    }

    NamedDomainObjectContainer<FlintStaticFileDescription> staticFileDescriptions =
        project.getExtensions().getByType(FlintGradleExtension.class).getStaticFiles().getStaticFileDescriptions();

    for(FlintStaticFileDescription staticFileDescription : staticFileDescriptions) {
      staticFileDescription.validate();

      String targetPath = staticFileDescription.getTarget();
      if(staticFileDescription.isRemote()) {
        // Remote file, skip checksum computation
        // we assume remote files only change when their URL changes
        remoteFiles.add(new ManifestStaticFile(
            staticFileDescription.getSourceURI(),
            staticFileDescription.getOperatingSystem(),
            targetPath
        ));
      } else {
        // Local file, generate an URL
        URI remoteURI = generateURIForLocalFile(staticFileDescription.getName(), configurator);
        localFiles.put(
            staticFileDescription.getSourceFile(),
            new ManifestStaticFile(remoteURI, staticFileDescription.getOperatingSystem(), targetPath)
        );
      }
    }

    computed = true;
  }

  /**
   * Generates the distributor URL for a static file entry.
   *
   * @param name         The name of the static file
   * @param configurator The manifest configurator currently computing the inputs
   * @return The generated URI
   */
  private URI generateURIForLocalFile(String name, ManifestConfigurator configurator) {
    URI projectMavenURI = configurator.getProjectMavenURI(
        "Remove static file entries, a distributor is URL is required to use static files");

    return Util.concatURI(projectMavenURI, name);
  }

  public Set<ManifestStaticFile> getRemoteFiles() {
    if(!computed) {
      throw new IllegalStateException("Input has not been computed yet");
    }

    return remoteFiles;
  }

  public Map<File, ManifestStaticFile> getLocalFiles() {
    if(!computed) {
      throw new IllegalStateException("Input has not been computed yet");
    }

    return localFiles;
  }
}
