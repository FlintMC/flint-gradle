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

package net.flintmc.gradle.manifest;

import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.manifest.data.ManifestMavenDependencyInput;
import net.flintmc.gradle.manifest.data.ManifestPackageDependencyInput;
import net.flintmc.gradle.manifest.data.ManifestRepositoryInput;
import net.flintmc.gradle.manifest.data.ManifestStaticFileInput;
import net.flintmc.gradle.manifest.tasks.*;
import net.flintmc.gradle.maven.cache.MavenArtifactURLCache;
import net.flintmc.gradle.minecraft.InstallStaticFilesTask;
import net.flintmc.gradle.property.FlintPluginProperties;
import net.flintmc.gradle.util.MaybeNull;
import net.flintmc.gradle.util.Util;
import okhttp3.OkHttpClient;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.credentials.HttpHeaderCredentials;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.Copy;
import org.gradle.authentication.http.HttpHeaderAuthentication;

import java.io.File;
import java.net.URI;

public class ManifestConfigurator {
  private final Project project;
  private final OkHttpClient httpClient;
  private final MavenArtifactURLCache mavenArtifactURLCache;

  /**
   * Constructs a new {@link ManifestConfigurator} for the given plugin.
   *
   * @param plugin The plugin to configure the manifest for
   */
  public ManifestConfigurator(FlintGradlePlugin plugin) {
    this.project = plugin.getProject();
    this.httpClient = plugin.getHttpClient();
    this.mavenArtifactURLCache = plugin.getMavenArtifactURLCache();
  }

  private URI projectPublishURI;
  private URI distributorMavenURI;
  private URI projectMavenURI;

  /**
   * Installs the required gradle tasks to generate the flint manifests.
   */
  public void configure() {
    if(!isValidProject(project)) {
      return;
    }

    FlintGradleExtension extension = project.getExtensions().getByType(FlintGradleExtension.class);

    if(extension.shouldAutoConfigurePublishing() && extension.shouldEnablePublishing()) {
      // Auto configuration is enabled
      PublishingExtension publishingExtension = project.getExtensions().findByType(PublishingExtension.class);

      // Build the distributor URL in form of <host>/maven/<channel>
      URI distributorUrl = getDistributorMavenURI(
          "Set enablePublishing to false in the flint extension",
          "Set shouldAutoConfigurePublishing to false in the flint extension");

      if(publishingExtension != null) {
        // Found a publishing extension, automatically set the publish target
        MavenPublication publication = publishingExtension.getPublications().create("flint", MavenPublication.class);

        // Configure the publication
        publication.setGroupId(project.getGroup().toString());
        publication.setArtifactId(project.getName());
        publication.setVersion(project.getVersion().toString());

        // Add all components to the publication
        publication.from(project.getComponents().getByName("java"));

        // Configure the repository
        MavenArtifactRepository repository =
            publishingExtension.getRepositories().maven((repo) -> repo.setName("FlintDistributor"));
        repository.setUrl(distributorUrl);

        // Try to retrieve authentication, it might not be available
        HttpHeaderCredentials values = Util.getPublishCredentials(project, false);

        if(values != null) {
          // Gradle does not allow setting the credentials instance directly, so copy it
          HttpHeaderCredentials credentials = repository.getCredentials(HttpHeaderCredentials.class);
          credentials.setName(values.getName());
          credentials.setValue(values.getValue());
        }

        // Set the authentication, no further configuration required
        repository.getAuthentication().create("header", HttpHeaderAuthentication.class);
      }
    }

    // Create the task inputs
    ManifestMavenDependencyInput mavenDependencyInput = new ManifestMavenDependencyInput();
    ManifestRepositoryInput repositoryInput = new ManifestRepositoryInput();
    ManifestStaticFileInput staticFileInput = new ManifestStaticFileInput(this);
    ManifestPackageDependencyInput packageDependencyInput = new ManifestPackageDependencyInput();

    // Create the tasks
    ResolveArtifactURLsTask resolveArtifactURLsTask = project.getTasks().create(
        "resolveArtifactURLs",
        ResolveArtifactURLsTask.class,
        new MaybeNull<>(httpClient),
        mavenArtifactURLCache,
        repositoryInput,
        mavenDependencyInput
    );
    resolveArtifactURLsTask.setGroup("publishing");
    resolveArtifactURLsTask.setDescription("Resolves the URLs of all package dependency artifacts and caches them");

    GenerateStaticFileChecksumsTask generateStaticFileChecksumsTask = project.getTasks().create(
        "generateStaticFileChecksums",
        GenerateStaticFileChecksumsTask.class,
        new MaybeNull<>(httpClient),
        staticFileInput
    );
    generateStaticFileChecksumsTask.setGroup("publishing");
    generateStaticFileChecksumsTask.setDescription("Calculates the checksums of all static files and caches them");

    File manifestFile = new File(Util.getProjectCacheDir(project), "manifest.json");

    GenerateFlintManifestTask generateFlintManifestTask = project.getTasks().create(
        "generateFlintManifest",
        GenerateFlintManifestTask.class,
        manifestFile,
        staticFileInput,
        packageDependencyInput,
        resolveArtifactURLsTask.getCacheFile(),
        generateStaticFileChecksumsTask.getCacheFile()
    );
    generateFlintManifestTask.setGroup("publishing");
    generateFlintManifestTask.setDescription("Generates the flint manifest.json and caches it");
    generateFlintManifestTask.dependsOn(resolveArtifactURLsTask, generateStaticFileChecksumsTask);

    // Retrieve the process resources task so we can include the manifest
    // The processResources task is a copy task, and as the ProcessResources class is marked unstable,
    // we cast it to a copy task
    Copy processResourcesTask = (Copy) project.getTasks().getByName("processResources");
    processResourcesTask.from(manifestFile);
    processResourcesTask.dependsOn(generateFlintManifestTask);

    if(extension.shouldEnablePublishing()) {
      // Generate the URI to publish the manifest to
      URI manifestURI = Util.concatURI(
          getProjectPublishURI("Set enablePublishing to false in the flint extension"),
          "manifest.json"
      );

      // Create the manifest publish task
      PublishFileTask publishManifestTask = project.getTasks().create(
          "publishFlintManifest",
          PublishFileTask.class,
          this,
          new MaybeNull<>(httpClient),
          manifestFile,
          manifestURI
      );
      publishManifestTask.setGroup("publishing");
      publishManifestTask.setDescription("Publishes the flint manifest.json to the distributor");
      publishManifestTask.dependsOn(generateFlintManifestTask);

      // Create the static files publish task
      PublishStaticFilesTask publishStaticFilesTask = project.getTasks().create(
          "publishFlintStaticFiles",
          PublishStaticFilesTask.class,
          this,
          new MaybeNull<>(httpClient),
          staticFileInput,
          generateStaticFileChecksumsTask.getCacheFile()
      );
      publishStaticFilesTask.setGroup("publishing");
      publishStaticFilesTask.setDescription("Publishes the static files to the distributor");
      publishStaticFilesTask.dependsOn(generateStaticFileChecksumsTask);

      // Create a compound task
      Task publishFlintPackageMetaTask = project.getTasks().create("publishFlintPackageMeta");
      publishFlintPackageMetaTask.setGroup("publishing");
      publishFlintPackageMetaTask.setDescription("Compound task which depends on other publish tasks");
      publishFlintPackageMetaTask.dependsOn(publishManifestTask, publishStaticFilesTask);

      // Add dependency for the publish task
      project.getTasks().getByName("publish").dependsOn(publishFlintPackageMetaTask);
    }
  }

  /**
   * Determines whether the given project is valid flint project.
   *
   * @param project The project to test
   * @return {@code true} if the project is a valid flint project, {@code false} otherwise
   */
  private boolean isValidProject(Project project) {
    FlintGradleExtension remote = project.getExtensions().findByType(FlintGradleExtension.class);
    return remote != null && remote.getProjectFilter().test(project);
  }

  /**
   * Retrieves the base URI of the distributor publish endpoint including the project namespace.
   *
   * @param notAvailableSolutions Messages to display as a solution in case the URI can't be computed
   * @return The base URI of the distributor publish endpoint including the project namespace
   */
  public URI getProjectPublishURI(String... notAvailableSolutions) {
    if(projectPublishURI == null) {
      projectPublishURI = Util.concatURI(
          FlintPluginProperties.DISTRIBUTOR_URL.require(project, notAvailableSolutions),
          "api/v1/publish",
          FlintPluginProperties.DISTRIBUTOR_CHANNEL.require(project, notAvailableSolutions),
          project.getGroup().toString().replace('.', '/'),
          project.getName(),
          project.getVersion().toString()
      );
    }

    return projectPublishURI;
  }

  /**
   * Retrieves the base URI of the distributor repository.
   *
   * @param notAvailableSolution Messages to display as a solution in case URI can't be computed
   * @return The base URI of the distributor repository
   */
  public URI getDistributorMavenURI(String... notAvailableSolution) {
    if(distributorMavenURI == null) {
      distributorMavenURI = Util.concatURI(
          FlintPluginProperties.DISTRIBUTOR_URL
              .require(project, notAvailableSolution),
          "api/v1/maven",
          FlintPluginProperties.DISTRIBUTOR_CHANNEL
              .require(project, notAvailableSolution)
      );
    }

    return distributorMavenURI;
  }

  /**
   * Retrieves the base URI of the distributor repository including the project namespace.
   *
   * @param notAvailableSolution Messages to display as a solution in case the URI can't be computed
   * @return The base URI of the distributor repository including the project namespace
   */
  public URI getProjectMavenURI(String... notAvailableSolution) {
    if(projectMavenURI == null) {
      URI distributorURI = getDistributorMavenURI(notAvailableSolution);

      projectMavenURI = Util.concatURI(
          distributorURI,
          project.getGroup().toString().replace('.', '/'),
          project.getName(),
          project.getVersion().toString()
      );
    }

    return projectMavenURI;
  }

}
