package net.flintmc.gradle;

import net.flintmc.gradle.environment.DeobfuscationEnvironment;
import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.jar.JarTaskProvider;
import net.flintmc.gradle.java.JavaPluginInteraction;
import net.flintmc.gradle.java.RunConfigurationProvider;
import net.flintmc.gradle.manifest.ManifestConfigurator;
import net.flintmc.gradle.maven.MavenArtifactDownloader;
import net.flintmc.gradle.maven.RemoteMavenRepository;
import net.flintmc.gradle.maven.SimpleMavenRepository;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.minecraft.MinecraftRepository;
import net.flintmc.gradle.minecraft.yggdrasil.YggdrasilAuthenticator;
import net.flintmc.gradle.publish.AssetPublisher;
import net.flintmc.gradle.publish.PublishTaskProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.invocation.Gradle;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collection;

public class FlintGradlePlugin implements Plugin<Project> {
  public static final String MINECRAFT_TASK_GROUP = "minecraft";

  private static final String MINECRAFT_MAVEN = "https://libraries.minecraft.net";
  private static final String PUBLISH_BASE_URL = "http://dist.laby.tech:8080";

  private Project project;

  private HttpClient httpClient;
  private MavenArtifactDownloader downloader;

  private FlintGradleExtension extension;
  private JavaPluginInteraction interaction;
  private MinecraftRepository minecraftRepository;
  private SimpleMavenRepository internalRepository;
  private YggdrasilAuthenticator authenticator;
  private RunConfigurationProvider runConfigurationProvider;
  private JarTaskProvider jarTaskProvider;
  private AssetPublisher assetPublisher;
  private ManifestConfigurator manifestConfigurator;
  private PublishTaskProvider publishTaskProvider;

  private FlintGradlePlugin parentPlugin;

  @Override
  public void apply(@Nonnull Project project) {
    this.project = project;
    String[] versions = {"v1.15.2", "v1.16.3", "internal"};
    project.getPlugins().apply("maven-publish");


    for (String version : versions) {
      project.getConfigurations().maybeCreate(String.format("%sAnnotationProcessor", version.replace('.', '_')));
      Configuration versionedConfiguration = project.getConfigurations().maybeCreate(String.format("%sImplementation", version.replace('.', '_')));

      project.beforeEvaluate((dummy) -> {
        Configuration runtimeClasspathConfiguration = project.getConfigurations().getByName("runtimeClasspath");
        runtimeClasspathConfiguration.extendsFrom(versionedConfiguration);
      });
    }

    project.afterEvaluate(p -> onAfterEvaluate());

    Project parent = project;
    while ((parent = parent.getParent()) != null) {
      FlintGradlePlugin parentPlugin = parent.getPlugins().findPlugin(getClass());
      if (parentPlugin == null) continue;
      this.parentPlugin = parentPlugin;
      break;
    }

    this.interaction = new JavaPluginInteraction(project);

    if (this.parentPlugin == null) {

      System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

      System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
      System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");
      System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");


      Gradle gradle = project.getGradle();
      httpClient = gradle.getStartParameter().isOffline() ? null :
          HttpClientBuilder.create().useSystemProperties().build();

      downloader = new MavenArtifactDownloader();

      if (httpClient != null) {
        downloader.addSource(new RemoteMavenRepository(httpClient, "https://libraries.minecraft.net"));
        downloader.addSource(new RemoteMavenRepository(httpClient, "https://repo.maven.apache.org/maven2/"));
      }

      this.extension = project.getExtensions().create(FlintGradleExtension.NAME, FlintGradleExtension.class, this);

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
          project, minecraftRepository, minecraftCache.resolve("run"), authenticator);
      this.jarTaskProvider = new JarTaskProvider(this);
      this.assetPublisher = new AssetPublisher(PUBLISH_BASE_URL);
      this.manifestConfigurator = new ManifestConfigurator();
      this.publishTaskProvider = new PublishTaskProvider();
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
      this.assetPublisher = parentPlugin.assetPublisher;
      this.manifestConfigurator = parentPlugin.manifestConfigurator;
      this.publishTaskProvider = parentPlugin.publishTaskProvider;
    }

    project.afterEvaluate((p) -> extension.ensureConfigured());
  }

  /**
   * Called by the {@link FlintGradleExtension} as soon as it has been configured
   */
  public void onExtensionConfigured() {
    interaction.setup(extension);

    for (String version : extension.getMinecraftVersions()) {
      handleVersion(version);
    }

    project.getRepositories().maven(repo -> {
      repo.setUrl("Mojang");
      repo.setUrl(MINECRAFT_MAVEN);
    });

    project.getRepositories().maven((repo) -> {
      repo.setUrl("Internal minecraft");
      repo.setUrl(minecraftRepository.getBaseDir());
    });

    for (Project subProject : project.getSubprojects()) {
      if (!extension.getProjectFilter().test(subProject)) {
        continue;
      }
      subProject.getRepositories().maven(repo -> {
        repo.setUrl("Mojang");
        repo.setUrl(MINECRAFT_MAVEN);
      });

      subProject.getPluginManager().apply(getClass());
    }

    runConfigurationProvider.installSourceSets(project, extension);
    jarTaskProvider.installTasks(project, extension);
    manifestConfigurator.configureProject(project);
    publishTaskProvider.installPublishTask(project);
  }

  public void onAfterEvaluate() {
  }

  /**
   * Handles the given minecraft version and sets up all of the required steps for using it with gradle.
   *
   * @param version The minecraft version to handle
   */
  private void handleVersion(String version) {
    // Get the default obfuscation environment, we don't support custom environments currently
    DeobfuscationEnvironment environment = minecraftRepository.defaultEnvironment(version);

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
    interaction.setupVersioned(extension, compileArtifacts, runtimeArtifacts, version);
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
  private boolean allInstalled(Collection<MavenArtifact> artifacts, SimpleMavenRepository repository) {
    boolean allInstalled = true;

    for (MavenArtifact artifact : artifacts) {
      // Simply execute a logical or to build a chain of checks
      allInstalled = repository.isInstalled(artifact);

      if (!allInstalled) {
        // Avoid further check to save time
        break;
      }
    }

    return allInstalled;
  }

  /**
   * Retrieves the HTTP client the plugin uses for downloading files.
   *
   * @return The HTTP client of this plugin
   */
  public HttpClient getHttpClient() {
    return httpClient;
  }

  public AssetPublisher getAssetPublisher() {
    return assetPublisher;
  }

  public FlintGradleExtension getExtension() {
    return extension;
  }

  public String getPublishBaseUrl() {
    return PUBLISH_BASE_URL;
  }

  public Project getProject() {
    return project;
  }
}
