package net.flintmc.gradle.manifest.data;

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.util.Util;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.tasks.Input;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Cacheable list of maven dependencies.
 */
public class ManifestMavenDependencyInput {
  private final Set<ManifestMavenDependency> dependencies;

  private boolean computed;

  public ManifestMavenDependencyInput() {
    this.dependencies = new HashSet<>();
  }

  /**
   * Computes the input for the given project.
   *
   * @param project The project to compute the input for
   */
  public void compute(Project project) {
    if(computed) {
      return;
    }

    Set<ResolvedArtifact> runtimeClasspath =
        project.getConfigurations().getByName("runtimeClasspath").getResolvedConfiguration().getResolvedArtifacts();

    for(ResolvedArtifact resolvedArtifact : runtimeClasspath) {
      ComponentIdentifier componentIdentifier = resolvedArtifact.getId().getComponentIdentifier();

      if(componentIdentifier instanceof ModuleComponentIdentifier) {
        // Check if the artifact is a package
        File artifactFile = resolvedArtifact.getFile();
        try {
          if(Util.isPackageJar(artifactFile)) {
            // Package jars are handled differently
            continue;
          }
        } catch(IOException e) {
          throw new FlintGradleException(
              "Failed to check if file " + artifactFile.getAbsolutePath() + " is a package jar", e);
        }

        ModuleComponentIdentifier module = (ModuleComponentIdentifier) componentIdentifier;

        // Index the dependency
        dependencies.add(new ManifestMavenDependency(new MavenArtifact(
            module.getGroup(),
            module.getModule(),
            module.getVersion(),
            resolvedArtifact.getClassifier(),
            resolvedArtifact.getExtension()
        )));
      }
    }

    computed = true;
  }

  /**
   * Retrieves a set of all dependencies of the project.
   *
   * @return A set of all dependencies of the project
   */
  @Input
  public Set<ManifestMavenDependency> getDependencies() {
    if(!computed) {
      throw new IllegalStateException("Input has not been computed yet");
    }

    return dependencies;
  }
}
