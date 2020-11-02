package net.flintmc.gradle.manifest;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.extension.FlintGradleExtension;
import net.flintmc.gradle.maven.MavenArtifactDownloader;
import net.flintmc.gradle.maven.RemoteMavenRepository;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

/**
 * Creates a task to generate and publish the installer manifest json file.
 */
public class ManifestGeneratorOld {

  private final FlintGradlePlugin flintGradlePlugin;

  public ManifestGeneratorOld(FlintGradlePlugin flintGradlePlugin) {
    this.flintGradlePlugin = flintGradlePlugin;
  }

  public void installManifestGenerateTask(Project project) {
    project
        .getTasks()
        .create(
            "generateManifest",
            task -> {
              task.setGroup("publish");
              task.doLast(
                  task1 -> {
                    Manifest manifest = this.collectManifest(project);

                    task1.getExtensions().add("manifest", manifest);
                  });
            });

    project
        .getTasks()
        .create(
            "publishManifest",
            task -> {
              task.dependsOn("generateManifest");
              task.dependsOn("jar");
              task.setGroup("publish");

              task.doLast(
                  task1 -> {
                    try {

                      FlintGradleExtension extension = project.getExtensions().findByType(FlintGradleExtension.class);
                      if (extension == null) return;
                      if (!extension.getProjectFilter().test(project)) return;

                      String manifest = new ObjectMapper()
                          .writerWithDefaultPrettyPrinter()
                          .writeValueAsString(
                              project
                                  .getTasks()
                                  .getByName("generateManifest")
                                  .getExtensions()
                                  .getByName("manifest"));

                      HttpPut httpPut = new HttpPut(String.format("http://localhost:8080/api/v1/maven/%s/%s/%s/manifest.json", project.getGroup().toString().replace('.', '/'), project.getName(), project.getVersion().toString()));
                      httpPut.setEntity(new StringEntity(manifest));
                      httpPut.setHeader("Publish-Token", "PYxcKWa5sTWQrgLMb1Rg8GfH9a83iyb6f5JZzILpboJVeDzynjQpt1KkOdvpqR7y");
                      HttpResponse execute = this.flintGradlePlugin.getHttpClient().execute(httpPut);

                      InputStream content = execute.getEntity().getContent();
                      byte[] data = new byte[content.available()];
                      content.read(data);
                      String response = new String(data);
                      if (!response.equals("Ok"))
                        throw new AssertionError("Response code was not Ok" + System.lineSeparator() + response);

                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                  });
            });


  }


  /**
   * Collects all manifest downloads for dependencies that can be obtained by collectDependencies.
   *
   * @param project The project to obtain {@link ManifestDownload} from
   * @return The collected manifest
   */
  public Manifest collectManifest(Project project) {
    Configuration runtimeClasspath = project.getConfigurations().getByName("runtimeClasspath");

    MavenArtifactDownloader mavenArtifactDownloader = new MavenArtifactDownloader();

    for (ArtifactRepository repository : project.getRepositories()) {
      if (repository instanceof MavenArtifactRepository) {
        Collection<URI> uris = new HashSet<>(((MavenArtifactRepository) repository).getArtifactUrls());
        uris.add(((MavenArtifactRepository) repository).getUrl());
        for (URI uri : uris) {
          mavenArtifactDownloader.addSource(new RemoteMavenRepository(flintGradlePlugin.getHttpClient(), uri.toString()));
        }
      }
    }

    String nameSpace = System.getenv("FLINT_NAMESPACE");
    if (nameSpace == null) {
      nameSpace = "RELEASE";
    }
    nameSpace = nameSpace.toLowerCase();

    Collection<ManifestDownload> manifestDownloads = new HashSet<>();
    Collection<ManifestPackageDependency> packageDependencies = new HashSet<>();
    Collection<ManifestMavenDependency> mavenDependencies = new HashSet<>();

    for (ResolvedArtifact resolvedArtifact : runtimeClasspath.getResolvedConfiguration().getResolvedArtifacts()) {
      ComponentIdentifier componentIdentifier = resolvedArtifact.getId().getComponentIdentifier();
      if (componentIdentifier instanceof ModuleComponentIdentifier) {
        ModuleComponentIdentifier moduleComponentIdentifier = (ModuleComponentIdentifier) componentIdentifier;
        try {
          MavenArtifact mavenArtifact = new MavenArtifact(moduleComponentIdentifier.getGroup(), moduleComponentIdentifier.getModule(), moduleComponentIdentifier.getVersion(), resolvedArtifact.getClassifier(), resolvedArtifact.getType());
          URI artifactUri = mavenArtifactDownloader.findArtifactUri(mavenArtifact);
          mavenDependencies.add(new ManifestMavenDependency(mavenArtifact.getGroupId(), mavenArtifact.getArtifactId(), mavenArtifact.getVersion(), artifactUri.toString()));
        } catch (IOException | URISyntaxException e) {
          e.printStackTrace();
        }
      } else if (componentIdentifier instanceof ProjectComponentIdentifier) {
        Project targetProject = project.getRootProject().project(((ProjectComponentIdentifier) componentIdentifier).getProjectPath());

        FlintGradleExtension flintGradleExtension = targetProject.getExtensions().findByType(FlintGradleExtension.class);
        if (flintGradleExtension == null) {
          throw new RuntimeException("Project " + targetProject + " does not match and is not a flint package. We cannot handle that.. No Idea where tf to get it from");
        }
        if (flintGradleExtension.getProjectFilter().test(targetProject)) {
          if (flintGradleExtension.getType() == FlintGradleExtension.Type.PACKAGE) {
            packageDependencies.add(new ManifestPackageDependency(targetProject.getGroup().toString(), targetProject.getName(), targetProject.getVersion().toString(), nameSpace));
          } else {
            mavenDependencies.add(new ManifestMavenDependency(project.getGroup().toString(), project.getName(), project.getVersion().toString(), String.format("${FLINT_DISTRIBUTOR_URL}/%s/%s/%s/%s/%s-%s.jar", nameSpace, project.getGroup().toString().replace('.', '/'), project.getName(), project.getVersion().toString(), project.getName(), project.getVersion().toString())));
          }
        } else {
          throw new IllegalStateException("project filter does not match project");
        }
      }
    }

    FlintGradleExtension extension = project.getExtensions().findByType(FlintGradleExtension.class);
    return new Manifest(
        project.getName(),
        Optional.ofNullable(project.getDescription()).orElse(""),
        project.getVersion().toString(),
        extension != null ? extension.getAuthors() : new String[]{},
        manifestDownloads.toArray(new ManifestDownload[]{}),
        packageDependencies.toArray(new ManifestPackageDependency[]{}),
        mavenDependencies.toArray(new ManifestMavenDependency[]{}));
  }

}
