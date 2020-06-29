package net.labyfy.gradle;

import net.labyfy.gradle.extension.LabyfyGradleExtension;
import net.labyfy.gradle.java.JavaPluginInteraction;
import net.labyfy.gradle.minecraft.MinecraftRepository;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;

public class LabyfyGradlePlugin implements Plugin<Project> {
    private LabyfyGradleExtension extension;
    private JavaPluginInteraction interaction;
    private MinecraftRepository minecraftRepository;

    @Override
    public void apply(@Nonnull Project project) {
        this.interaction = new JavaPluginInteraction(project);
        this.extension = project.getExtensions().create(LabyfyGradleExtension.NAME, LabyfyGradleExtension.class, this);

        try {
            this.minecraftRepository = new MinecraftRepository(project.getGradle());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create minecraft repository", e);
        }
    }

    /**
     * Called by the {@link LabyfyGradleExtension} as soon as it has been configured
     */
    public void onExtensionConfigured() {
        interaction.setup(extension, minecraftRepository);

        for(String version : extension.getMinecraftVersions()) {
            try {
                minecraftRepository.install(version);
            } catch (IOException e) {
                throw new GradleException("Failed to install minecraft version " + version, e);
            }
        }
    }
}
