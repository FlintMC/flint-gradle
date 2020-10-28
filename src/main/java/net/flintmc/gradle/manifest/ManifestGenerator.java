package net.flintmc.gradle.manifest;

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.maven.RemoteMavenRepository;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import org.apache.commons.codec.digest.DigestUtils;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.internal.impldep.com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;

/**
 * Creates a task to generate and publish the installer manifest json file.
 */
public class ManifestGenerator {

  private final FlintGradlePlugin flintGradlePlugin;

  public ManifestGenerator(FlintGradlePlugin flintGradlePlugin) {
    this.flintGradlePlugin = flintGradlePlugin;
  }

  /**
   * Install a gradle task to generate and publish a projects manifest file.
   *
   * @param project The gradle plroject to install task in
   */
  public void installManifestGenerateTask(Project project) {
    //Creates manifest publish task
    project.getTasks().create("flintManifestPublish", task -> {
      task.setGroup("publish");
      task.dependsOn("jar");
      task.doLast(task1 -> {
        Manifest manifest = new Manifest()
            .setName(project.getName())
            .setVersion(project.getVersion().toString())
            .setDescription(project.getDescription())
            //Collect all project dependencies with configuration 'flint' and add it to the manifest
            .setDownloads(this.collectDownloads(project).toArray(new ManifestDownload[]{}));
        String manifestContent = new GsonBuilder().setPrettyPrinting().create().toJson(manifest);
        this.flintGradlePlugin.getAssetPublisher().publish(manifestContent.getBytes(Charset.defaultCharset()), project.getName(), project.getVersion().toString(), "manifest.json", this.flintGradlePlugin.getExtension().getPublishToken());
      });
    });

    //Does the same thing, but publishes the manifest with version 'latest' in another task
    project.getTasks().create("flintManifestLatestPublish", task -> {
      task.setGroup("publish");
      task.dependsOn("jar");
      task.doLast(task1 -> {
        Manifest manifest = new Manifest()
            .setName(project.getName())
            .setVersion(project.getVersion().toString())
            .setDescription(project.getDescription())
            //Collect all project dependencies with configuration 'flint' and add it to the manifest
            .setDownloads(this.collectDownloads(project).toArray(new ManifestDownload[]{}));
        String manifestContent = new GsonBuilder().setPrettyPrinting().create().toJson(manifest);
        this.flintGradlePlugin.getAssetPublisher().publish(manifestContent.getBytes(Charset.defaultCharset()), project.getName(), "latest", "manifest.json", this.flintGradlePlugin.getExtension().getPublishToken());
      });
    });
  }

  /**
   * Collects all manifest downloads for dependencies that can be obtained by collectDependencies.
   *
   * @param project The project to obtain {@link ManifestDownload} from
   * @return The collected manifest downloads
   */
  public Collection<ManifestDownload> collectDownloads(Project project) {
    File jar = project.getTasks().getByName("jar").getOutputs().getFiles().getSingleFile();
    Collection<ManifestDownload> manifestDownloads = this.collectDependencies(project);
    try {
      if (jar.exists())
        manifestDownloads.add(new ManifestDownload()
            .setUrl(flintGradlePlugin.getPublishBaseUrl() + "/package/" + project.getName() + "/" + project.getVersion().toString() + "/" + jar.getName())
            .setPath("Flint/packages/" + jar.getName())
            .setMd5(DigestUtils.md5Hex(Files.readAllBytes(jar.toPath()))));
    } catch (IOException e) {
      throw new FlintGradleException("Cannot read file " + jar, e);
    }
    return manifestDownloads;
  }

  /**
   * Collects all dependencies that can be obtained from dependency configuration 'flint'.
   *
   * @param project The project to obtain dependencies from
   * @return The collected project dependencies
   */
  public Collection<ManifestDownload> collectDependencies(Project project) {
    Collection<ManifestDownload> manifestDownloads = new HashSet<>();
    //Retrieve all dependencies from configuration 'flint'
    project.getConfigurations().maybeCreate("flint").getDependencies().forEach(dependency -> {

      //if dependency is a project dependency, get it from the distributor
      if (dependency instanceof ProjectDependency) {
        File jar = ((ProjectDependency) dependency)
            .getDependencyProject()
            .getConfigurations()
            .getByName("archives")
            .getOutgoing()
            .getArtifacts()
            .getFiles()
            .getSingleFile();

        try {
          if (jar.exists())
            //add to manifest downloads
            manifestDownloads.add(new ManifestDownload()
                .setUrl(flintGradlePlugin.getPublishBaseUrl() + "/package/" + dependency.getName() + "/" + dependency.getVersion() + "/" + jar.getName())
                .setPath("Flint/packages/" + jar.getName())
                .setMd5(DigestUtils.md5Hex(Files.readAllBytes(jar.toPath()))));
        } catch (IOException e) {
          throw new FlintGradleException("Cannot read file " + jar, e);
        }

        return;
      }

      //if project is a maven dependency get it from maven repository
      for (ArtifactRepository repository : project.getRepositories().getAsMap().values()) {
        try {
          String url = ((URI) DefaultGroovyMethods.getProperties(repository).get("url")).toURL().toExternalForm();
          RemoteMavenRepository remoteMavenRepository = new RemoteMavenRepository(this.flintGradlePlugin.getHttpClient(), url);
          MavenArtifact mavenArtifact = new MavenArtifact(dependency.getGroup(), dependency.getName(), dependency.getVersion());
          String path = remoteMavenRepository.buildArtifactPath(mavenArtifact, false);

          //check if resource exists
          try (InputStream inStream = remoteMavenRepository.getArtifactStream(mavenArtifact)) {
            if (inStream != null) {
              byte[] data = new byte[inStream.available()];
              inStream.read(data);

              //Add to manifest downloads
              manifestDownloads.add(new ManifestDownload()
                  .setUrl(url + (path.startsWith("/") ? path.substring(1) : path))
                  .setMd5(DigestUtils.md5Hex(data))
                  .setPath(String.format("Flint/libraries/%s/%s/%s/%s-%s.jar",
                      dependency.getGroup().replace('.', '/'), dependency.getName(), dependency.getVersion(),
                      dependency.getName(), dependency.getVersion()))
              );
              return;
            }
          }
        } catch (Exception ignored) {
        }
      }
    });
    return manifestDownloads;
  }

}