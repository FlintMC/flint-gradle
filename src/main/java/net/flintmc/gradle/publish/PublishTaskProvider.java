package net.flintmc.gradle.publish;

import org.gradle.api.Project;

public class PublishTaskProvider {

  /**
   * Installs a gradle task to publish the current project to the lm-distributor.
   *
   * @param project The project to install the task on
   */
  public void installPublishTask(Project project) {
    project.getTasks().create("publishFlint", task -> {
      task.setGroup("publish");
      task.dependsOn("publish", "publishFlintStaticFiles", "publishFlintManifest");
    });
  }

}
