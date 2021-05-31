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
import net.flintmc.gradle.property.FlintPluginProperties;
import net.flintmc.gradle.util.MaybeNull;
import net.flintmc.gradle.util.Util;
import okhttp3.OkHttpClient;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.Copy;
import org.gradle.jvm.tasks.Jar;

import java.io.File;
import java.net.URI;

public class ManifestConfigurator {
  private final Project project;
  private final OkHttpClient httpClient;
  private final MavenArtifactURLCache mavenArtifactURLCache;
  private final FlintGradlePlugin flintGradlePlugin;

  /**
   * Constructs a new {@link ManifestConfigurator} for the given plugin.
   *
   * @param plugin The plugin to configure the manifest for
   */
  public ManifestConfigurator(FlintGradlePlugin plugin) {
    this.flintGradlePlugin = plugin;
    this.project = plugin.getProject();
    this.httpClient = plugin.getHttpClient();
    this.mavenArtifactURLCache = plugin.getMavenArtifactURLCache();
  }

  private URI projectPublishURI;
  private URI projectMavenURI;

  /**
   * Installs the required gradle tasks to generate the flint manifests.
   */
  public void configure() {
    if (!isValidProject(project)) {
      return;
    }

    FlintGradleExtension extension = project.getExtensions().getByType(FlintGradleExtension.class);

    if (extension.shouldAutoConfigurePublishing() && extension.shouldEnablePublishing()) {
      // Auto configuration is enabled
      PublishingExtension publishingExtension = project.getExtensions().findByType(PublishingExtension.class);

      // Build the distributor URL in form of <host>/maven/<channel>
      URI distributorUrl = Util.getDistributorMavenURI(project,
          "Set enablePublishing to false in the flint extension",
          "Set shouldAutoConfigurePublishing to false in the flint extension");

      if (publishingExtension != null) {
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

        // Apply the access credentials if available
        Util.applyDistributorCredentials(project, repository, false);
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

    File manifestFileJar = new File(Util.getProjectCacheDir(project), "manifestJar.json");

    GenerateFlintManifestTask generateFlintManifestJarTask = project.getTasks().create(
        "generateFlintManifestJar",
        GenerateFlintManifestTask.class,
        this.flintGradlePlugin,
        GenerateFlintManifestTask.ManifestType.JAR,
        manifestFileJar,
        staticFileInput,
        packageDependencyInput,
        resolveArtifactURLsTask.getCacheFile(),
        generateStaticFileChecksumsTask.getCacheFile(),
        repositoryInput
    );
    generateFlintManifestJarTask.setGroup("publishing");
    generateFlintManifestJarTask.setDescription("Generates the flint manifest.json to include in the jar file and caches it");
    generateFlintManifestJarTask.dependsOn(resolveArtifactURLsTask, generateStaticFileChecksumsTask);

    File manifestFileDistributor = new File(Util.getProjectCacheDir(project), "manifestDistributor.json");

    GenerateFlintManifestTask generateFlintManifestDistributorTask = project.getTasks().create(
        "generateFlintManifestDistributor",
        GenerateFlintManifestTask.class,
        this.flintGradlePlugin,
        GenerateFlintManifestTask.ManifestType.DISTRIBUTOR,
        manifestFileDistributor,
        staticFileInput,
        packageDependencyInput,
        resolveArtifactURLsTask.getCacheFile(),
        generateStaticFileChecksumsTask.getCacheFile(),
        repositoryInput
    );
    Jar jar = (Jar) project.getTasks().getByName("jar");
    generateFlintManifestDistributorTask.setGroup("publishing");
    generateFlintManifestDistributorTask.setDescription("Generates the flint manifest.json to publish to the distributor and caches it");
    generateFlintManifestDistributorTask.dependsOn(resolveArtifactURLsTask, generateStaticFileChecksumsTask, jar);


    // Retrieve the process resources task so we can include the manifest
    // The processResources task is a copy task, and as the ProcessResources class is marked unstable,
    // we cast it to a copy task
    Copy processResourcesTask = (Copy) project.getTasks().getByName("processResources");
    processResourcesTask
        .from(manifestFileJar)
        .rename("manifestJar.json", "manifest.json");
    processResourcesTask.dependsOn(generateFlintManifestJarTask);

    if (extension.shouldEnablePublishing()) {
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
          manifestFileDistributor,
          manifestURI
      );
      publishManifestTask.setGroup("publishing");
      publishManifestTask.setDescription("Publishes the flint manifest.json to the distributor");
      publishManifestTask.dependsOn(generateFlintManifestJarTask);
      publishManifestTask.dependsOn(generateFlintManifestDistributorTask);

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
    if (projectPublishURI == null) {
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
   * Retrieves the base URI of the distributor repository including the project namespace.
   *
   * @param notAvailableSolution Messages to display as a solution in case the URI can't be computed
   * @return The base URI of the distributor repository including the project namespace
   */
  public URI getProjectMavenURI(String... notAvailableSolution) {
    if (projectMavenURI == null) {
      URI distributorURI = Util.getDistributorMavenURI(project, notAvailableSolution);

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
