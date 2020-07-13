package net.labyfy.gradle.manifest;

import net.labyfy.gradle.LabyfyGradlePlugin;
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
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashSet;

/**
 * Creates a task to generate and publish the installer manifest json file.
 */
public class ManifestGenerator {

  private final LabyfyGradlePlugin labyfyGradlePlugin;

  public ManifestGenerator(LabyfyGradlePlugin labyfyGradlePlugin) {
    this.labyfyGradlePlugin = labyfyGradlePlugin;
  }

  /**
   * Install a gradle task to generate and publish a projects manifest file.
   *
   * @param project The gradle plroject to install task in
   */
  public void installManifestGenerateTask(Project project) {
    project.getTasks().create("labyfyManifestPublish", task -> {
      task.setGroup("publish");
      task.dependsOn("jar");
      task.doLast(task1 -> {
        Manifest manifest = new Manifest()
            .setName(project.getName())
            .setVersion(project.getVersion().toString())
            .setDescription(project.getDescription())
            .setDownloads(this.collectDownloads(project).toArray(new ManifestDownload[]{}));
        String manifestContent = new GsonBuilder().setPrettyPrinting().create().toJson(manifest);
        this.labyfyGradlePlugin.getAssetPublisher().publish(manifestContent.getBytes(Charset.defaultCharset()), project.getName(), project.getVersion().toString(), "manifest.json", this.labyfyGradlePlugin.getExtension().getPublishToken());
      });
    });

    project.getTasks().create("labyfyManifestLatestPublish", task -> {
      task.setGroup("publish");
      task.dependsOn("jar");
      task.doLast(task1 -> {
        Manifest manifest = new Manifest()
            .setName(project.getName())
            .setVersion(project.getVersion().toString())
            .setDescription(project.getDescription())
            .setDownloads(this.collectDownloads(project).toArray(new ManifestDownload[]{}));
        String manifestContent = new GsonBuilder().setPrettyPrinting().create().toJson(manifest);
        this.labyfyGradlePlugin.getAssetPublisher().publish(manifestContent.getBytes(Charset.defaultCharset()), project.getName(), "latest", "manifest.json", this.labyfyGradlePlugin.getExtension().getPublishToken());
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
            .setUrl(labyfyGradlePlugin.getPublishBaseUrl() + "/package/" + project.getName() + "/" + project.getVersion().toString() + "/" + jar.getName())
            .setPath("Labyfy/packages/" + jar.getName())
            .setMd5(DigestUtils.md5Hex(Files.readAllBytes(jar.toPath()))));
    } catch (IOException e) {
      e.printStackTrace();
    }
    return manifestDownloads;
  }

  /**
   * Collects all dependencies that can be obtained from dependency configuration 'labyfy'.
   *
   * @param project The project to obtain dependencies from
   * @return The collected project dependencies
   */
  public Collection<ManifestDownload> collectDependencies(Project project) {
    Collection<ManifestDownload> manifestDownloads = new HashSet<>();
    project.getConfigurations().maybeCreate("labyfy").getDependencies().forEach(dependency -> {

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
            manifestDownloads.add(new ManifestDownload()
                .setUrl(labyfyGradlePlugin.getPublishBaseUrl() + "/package/" + dependency.getName() + "/" + dependency.getVersion().toString() + "/" + jar.getName())
                .setPath("Labyfy/packages/" + jar.getName())
                .setMd5(DigestUtils.md5Hex(Files.readAllBytes(jar.toPath()))));
        } catch (IOException e) {
          e.printStackTrace();
        }

        return;
      }

      for (ArtifactRepository repository : project.getRepositories().getAsMap().values()) {
        try {
          URL url = ((URI) DefaultGroovyMethods.getProperties(repository).get("url")).toURL();
          String jarUrl = String.format("%s%s/%s/%s/%s-%s.jar", url.toString(),
              dependency.getGroup().replace('.', '/'), dependency.getName(), dependency.getVersion(),
              dependency.getName(), dependency.getVersion());
          URL jarfile = new URL(jarUrl);
          try (InputStream inStream = jarfile.openStream()) {
            if (inStream != null) {
              byte[] data = new byte[inStream.available()];
              inStream.read(data);

              manifestDownloads.add(new ManifestDownload()
                  .setUrl(jarUrl)
                  .setMd5(DigestUtils.md5Hex(data))
                  .setPath(String.format("Labyfy/libraries/%s/%s/%s/%s-%s.jar",
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
