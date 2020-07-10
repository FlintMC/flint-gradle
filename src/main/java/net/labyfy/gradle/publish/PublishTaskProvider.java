package net.labyfy.gradle.publish;

import org.gradle.api.Project;

public class PublishTaskProvider {

  public void installPublishTask(Project project) {
    project.getTasks().create("labyfyPublish", task -> {
      task.setGroup("publish");
      task.dependsOn("labyfyManifestPublish", "labyfyArtifactPublish", "labyfyManifestLatestPublish");
    });
  }

}
