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

package net.flintmc.gradle;

import net.flintmc.gradle.environment.DeobfuscationEnvironment;
import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.extension.FlintPatcherExtension;
import net.flintmc.gradle.extension.FlintStaticFileDescription;
import net.flintmc.gradle.java.JarTaskProvider;
import net.flintmc.gradle.java.JavaPluginInteraction;
import net.flintmc.gradle.java.RunConfigurationProvider;
import net.flintmc.gradle.manifest.ManifestConfigurator;
import net.flintmc.gradle.manifest.dev.DevelopmentStaticFiles;
import net.flintmc.gradle.maven.FlintResolutionStrategy;
import net.flintmc.gradle.maven.MavenArtifactDownloader;
import net.flintmc.gradle.maven.RemoteMavenRepository;
import net.flintmc.gradle.maven.SimpleMavenRepository;
import net.flintmc.gradle.maven.cache.MavenArtifactURLCache;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.minecraft.MinecraftRepository;
import net.flintmc.gradle.minecraft.data.environment.EnvironmentType;
import net.flintmc.gradle.minecraft.data.environment.MinecraftVersion;
import net.flintmc.gradle.minecraft.yggdrasil.YggdrasilAuthenticator;
import net.flintmc.gradle.util.JavaClosure;
import net.flintmc.gradle.util.Util;
import okhttp3.OkHttpClient;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.Delete;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;

public class FlintGradlePlugin implements Plugin<Project> {
  public static final String MINECRAFT_TASK_GROUP = "minecraft";

  private static final String MINECRAFT_MAVEN = "https://libraries.minecraft.net";
  private static final String MAVEN_CENTRAL = "https://repo.maven.apache.org/maven2/";

  private Project project;

  private OkHttpClient httpClient;
  private MavenArtifactDownloader downloader;
  private MavenArtifactURLCache mavenArtifactURLCache;

  private FlintGradleExtension extension;
  private JavaPluginInteraction interaction;
  private MinecraftRepository minecraftRepository;
  private SimpleMavenRepository internalRepository;
  private YggdrasilAuthenticator authenticator;
  private RunConfigurationProvider runConfigurationProvider;
  private JarTaskProvider jarTaskProvider;
  private ManifestConfigurator manifestConfigurator;

  private FlintGradlePlugin parentPlugin;

  @Override
  public void apply(@Nonnull Project project) {
    this.project = project;
    project.getPlugins().apply("maven-publish");

    Project parent = project;
    while ((parent = parent.getParent()) != null) {
      FlintGradlePlugin parentPlugin = parent.getPlugins().findPlugin(getClass());
      if (parentPlugin == null) {
        continue;
      }
      this.parentPlugin = parentPlugin;
      break;
    }

    this.interaction = new JavaPluginInteraction(project);

    if (this.parentPlugin == null) {
      Gradle gradle = project.getGradle();
      httpClient = gradle.getStartParameter().isOffline() ? null :
          new OkHttpClient.Builder().build();

      downloader = new MavenArtifactDownloader();

      if (httpClient != null) {
        downloader.addSource(new RemoteMavenRepository(httpClient, URI.create(MINECRAFT_MAVEN)));
        downloader.addSource(new RemoteMavenRepository(httpClient, URI.create(MAVEN_CENTRAL)));
      }

      this.extension = project.getExtensions().create(FlintGradleExtension.NAME, FlintGradleExtension.class, this);
      project.getExtensions().create(FlintPatcherExtension.NAME, FlintPatcherExtension.class, this);

      Path flintGradlePath = gradle.getGradleUserHomeDir().toPath().resolve("caches/flint-gradle");
      Path minecraftCache = flintGradlePath.resolve("minecraft-cache");

      try {
        this.minecraftRepository = new MinecraftRepository(
            flintGradlePath.resolve("minecraft-repository"),
            minecraftCache,
            httpClient
        );

        this.internalRepository = new SimpleMavenRepository(flintGradlePath.resolve("internal-repository"));
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to create minecraft repository", e);
      }

      try {
        this.authenticator = httpClient != null ?
            new YggdrasilAuthenticator(httpClient, flintGradlePath.resolve("yggdrasil")) :
            null;
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to create Yggdrasil authenticator", e);
      }

      this.runConfigurationProvider = new RunConfigurationProvider(
          project, minecraftRepository, minecraftCache.resolve("run"), authenticator, httpClient);
      this.jarTaskProvider = new JarTaskProvider();
      this.mavenArtifactURLCache = new MavenArtifactURLCache(flintGradlePath.resolve("maven-artifact-urls"), httpClient == null);
      try {
        this.mavenArtifactURLCache.setup();
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to setup artifact URL cache", e);
      }

    } else {
      this.httpClient = parentPlugin.httpClient;
      this.downloader = parentPlugin.downloader;
      this.minecraftRepository = parentPlugin.minecraftRepository;
      this.internalRepository = parentPlugin.internalRepository;
      this.extension = project.getExtensions().create(
          FlintGradleExtension.NAME, FlintGradleExtension.class, this, parentPlugin.extension);
      this.authenticator = parentPlugin.authenticator;
      this.runConfigurationProvider = parentPlugin.runConfigurationProvider;
      this.jarTaskProvider = parentPlugin.jarTaskProvider;
      this.mavenArtifactURLCache = parentPlugin.mavenArtifactURLCache;
    }

    this.manifestConfigurator = new ManifestConfigurator(this);
    interaction.setup();
    project.getTasks().getByName("clean").configure(
        JavaClosure.of((Delete task) -> task.delete(Util.getProjectCacheDir(project))));

    project.afterEvaluate((p) -> extension.ensureConfigured());

    project.getRepositories().maven(repo -> {
      repo.setName("Mojang");
      repo.setUrl(MINECRAFT_MAVEN);
    });

    project.getRepositories().maven(repo -> {
      repo.setName("Flint");
      repo.setUrl(Util.getDistributorMavenURI(project));
      Util.applyDistributorCredentials(project, repo, false);
    });

    project.getRepositories().maven((repo) -> {
      repo.setName("Internal minecraft");
      repo.setUrl(minecraftRepository.getBaseDir());
    });
  }

  /**
   * Called by the {@link FlintGradleExtension} as soon as it has been configured
   */
  public void onExtensionConfigured() {
    if(extension.getFlintVersion() == null) {
      throw new IllegalStateException("Please set the flintVersion property on the flint extension");
    }

    for (MinecraftVersion minecraftVersion : extension.getMinecraftVersions()) {
      this.handleVersion(minecraftVersion.getVersion(), minecraftVersion.getEnvironmentType());
    }

    for(FlintStaticFileDescription staticFileDescription : extension.getStaticFiles().getStaticFileDescriptions()) {
      if(!staticFileDescription.isRemote()) {
        DevelopmentStaticFiles.register(
            project, staticFileDescription.getTarget(), staticFileDescription.getSourceFile());
      }
    }

    FlintResolutionStrategy.getInstance().forceResolutionStrategy(project, extension);

    for (Project subProject : project.getSubprojects()) {
      if (!extension.getProjectFilter().test(subProject)) {
        continue;
      }
      subProject.getRepositories().maven(repo -> {
        repo.setName("Mojang");
        repo.setUrl(MINECRAFT_MAVEN);
      });

      subProject.getPluginManager().apply(getClass());
    }


    runConfigurationProvider.installSourceSets(project, extension);
    jarTaskProvider.installTasks(project, extension);
    manifestConfigurator.configure();
  }

  /**
   * Handles the given minecraft version and sets up all of the required steps for using it with gradle.
   *
   * @param version The minecraft version to handle
   * @param type    The environment type.
   */
  private void handleVersion(String version, EnvironmentType type) {
    DeobfuscationEnvironment environment = minecraftRepository.defaultEnvironment(version, type);

    // Get the server and client artifacts
    MavenArtifact client = getClientArtifact(version);
    MavenArtifact server = getServerArtifact(version);

    // Retrieve the artifacts which will be required to set up the interaction
    Collection<MavenArtifact> compileArtifacts = environment.getCompileArtifacts(client, server);
    Collection<MavenArtifact> runtimeArtifacts = environment.getRuntimeArtifacts(client, server);

    if (!allInstalled(compileArtifacts, minecraftRepository) ||
        !allInstalled(runtimeArtifacts, minecraftRepository)) {
      try {
        // Some artifacts are missing, request installation with the given environment
        minecraftRepository.install(version, environment, internalRepository, downloader, project);
      } catch (IOException e) {
        throw new GradleException("Failed to install minecraft version " + version, e);
      }
    }

    // Configure the project dependencies and configurations for the given version
    interaction.setupVersioned(compileArtifacts, runtimeArtifacts, version);
  }

  /**
   * Retrieves the singleton instance of the internal flint maven repository.
   * Usually at ~/.gradle/caches/flint-gradle/internal-repository.
   *
   * @return The internal flint maven repository
   */
  public SimpleMavenRepository getInternalRepository() {
    return internalRepository;
  }

  /**
   * Retrieves the client artifact for the given version.
   *
   * @param version The version to retrieve the client artifact for
   * @return The client artifact for the given version
   */
  private MavenArtifact getClientArtifact(String version) {
    return new MavenArtifact("net.minecraft", "client", version);
  }

  /**
   * Retrieves the server artifact for the given version
   *
   * @param version The version to retrieve the server artifact for
   * @return The server artifact for the given version
   */
  private MavenArtifact getServerArtifact(String version) {
    return new MavenArtifact("net.minecraft", "server", version);
  }

  /**
   * Checks if all given artifacts are installed in the given repository.
   *
   * @param artifacts  The artifacts to check for
   * @param repository The repository to check in
   * @return {@code true} if all given artifacts are installed, {@code false} otherwise
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean allInstalled(Collection<MavenArtifact> artifacts, SimpleMavenRepository repository) {
    for (MavenArtifact artifact : artifacts) {
      if (!repository.isInstalled(artifact)) {
        // Avoid further check to save time
        return false;
      }
    }

    return true;
  }

  /**
   * Retrieves the HTTP client the plugin uses for downloading files.
   *
   * @return The HTTP client of this plugin, or {@code null}, when using the offline mode
   */
  public OkHttpClient getHttpClient() {
    return httpClient;
  }

  /**
   * Retrieves the maven artifact URL cache the plugin uses for caching maven artifact URL's.
   *
   * @return The maven artifact URL cache
   */
  public MavenArtifactURLCache getMavenArtifactURLCache() {
    return mavenArtifactURLCache;
  }

  public Project getProject() {
    return project;
  }

  /**
   * Retrieves the singleton instance of the maven artifact downloader.
   *
   * @return The singleton instance of the maven artifact downloader or null if gradle is in offline mode
   */
  public MavenArtifactDownloader getDownloader() {
    return this.downloader;
  }
}
