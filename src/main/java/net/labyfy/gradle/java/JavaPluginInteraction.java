package net.labyfy.gradle.java;

import net.labyfy.gradle.extension.LabyfyGradleExtension;
import net.labyfy.gradle.minecraft.MinecraftRepository;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class JavaPluginInteraction {
    private final Project project;

    public JavaPluginInteraction(Project project) {
        this.project = project;
        project.getPluginManager().apply(JavaPlugin.class);
    }

    /**
     * Configures the source sets depending on how the extension has been configured
     *
     * @param extension The configured extension
     */
    public void setup(LabyfyGradleExtension extension, MinecraftRepository repository) {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

        SourceSet implementationSourceSet;
        if(!extension.shouldDisableInternalSourceSet()) {
            // The internal source set is not disabled, create it and set it
            // as the source set containing the implementation
            implementationSourceSet = sourceSets.maybeCreate("internal");
            SourceSet mainSourceSet = sourceSets.getByName("main");

            // Add the output of the main source set to the internal source set
            implementationSourceSet.getRuntimeClasspath().plus(mainSourceSet.getRuntimeClasspath());
            implementationSourceSet.getCompileClasspath().plus(mainSourceSet.getCompileClasspath());
            implementationSourceSet.getRuntimeClasspath().plus(mainSourceSet.getOutput());
            implementationSourceSet.getCompileClasspath().plus(mainSourceSet.getOutput());
        } else {
            // The internal source set is disabled, set the main source set
            // as the source set containing the implementation
            implementationSourceSet = sourceSets.getByName("main");
        }
    }
}
