package net.labyfy.gradle;

import net.labyfy.gradle.extension.LabyfyGradleExtension;
import net.labyfy.gradle.java.JavaPluginInteraction;
import net.labyfy.gradle.maven.MavenArtifactDownloader;
import net.labyfy.gradle.maven.RemoteMavenRepository;
import net.labyfy.gradle.maven.SimpleMavenRepository;
import net.labyfy.gradle.minecraft.MinecraftRepository;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.invocation.Gradle;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

public class LabyfyGradlePlugin implements Plugin<Project> {
    private HttpClient httpClient;
    private MavenArtifactDownloader downloader;

    private LabyfyGradleExtension extension;
    private JavaPluginInteraction interaction;
    private MinecraftRepository minecraftRepository;
    private SimpleMavenRepository internalRepository;

    @Override
    public void apply(@Nonnull Project project) {
        Gradle gradle = project.getGradle();
        httpClient = gradle.getStartParameter().isOffline() ? null :
                HttpClientBuilder.create().useSystemProperties().build();
        downloader = new MavenArtifactDownloader();

        if(httpClient != null) {
            downloader.addSource(new RemoteMavenRepository(httpClient, "https://libraries.minecraft.net"));
            downloader.addSource(new RemoteMavenRepository(httpClient, "https://repo.maven.apache.org/maven2/"));
        }

        this.interaction = new JavaPluginInteraction(project);
        this.extension = project.getExtensions().create(LabyfyGradleExtension.NAME, LabyfyGradleExtension.class, this);

        Path labyfyGradlePath = gradle.getGradleUserHomeDir().toPath().resolve("caches/labyfy-gradle");

        try {
            this.minecraftRepository = new MinecraftRepository(
                    labyfyGradlePath.resolve("minecraft-repository"),
                    labyfyGradlePath.resolve("minecraft-cache"),
                    httpClient
            );

            this.internalRepository = new SimpleMavenRepository(labyfyGradlePath.resolve("internal-repository"));
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
                minecraftRepository.install(version, internalRepository, downloader);
            } catch (IOException e) {
                throw new GradleException("Failed to install minecraft version " + version, e);
            }
        }
    }
}
