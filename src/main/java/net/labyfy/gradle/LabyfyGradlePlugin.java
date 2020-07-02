package net.labyfy.gradle;

import net.labyfy.gradle.environment.DeobfuscationEnvironment;
import net.labyfy.gradle.extension.LabyfyGradleExtension;
import net.labyfy.gradle.java.JavaPluginInteraction;
import net.labyfy.gradle.maven.MavenArtifactDownloader;
import net.labyfy.gradle.maven.RemoteMavenRepository;
import net.labyfy.gradle.maven.SimpleMavenRepository;
import net.labyfy.gradle.maven.pom.MavenArtifact;
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
import java.util.Collection;

public class LabyfyGradlePlugin implements Plugin<Project> {
    private static final String MINECRAFT_MAVEN = "https://libraries.minecraft.net";

    private Project project;

    private HttpClient httpClient;
    private MavenArtifactDownloader downloader;

    private LabyfyGradleExtension extension;
    private JavaPluginInteraction interaction;
    private MinecraftRepository minecraftRepository;
    private SimpleMavenRepository internalRepository;

    @Override
    public void apply(@Nonnull Project project) {
        this.project = project;

        Gradle gradle = project.getGradle();
        httpClient = gradle.getStartParameter().isOffline() ? null :
                HttpClientBuilder.create().useSystemProperties().build();
        downloader = new MavenArtifactDownloader();

        if (httpClient != null) {
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
        interaction.setup(extension);

        for (String version : extension.getMinecraftVersions()) {
            handleVersion(version);
        }

        project.getRepositories().maven((repo) -> {
            repo.setUrl("Mojang");
            repo.setUrl(MINECRAFT_MAVEN);
        });

        project.getRepositories().maven((repo) -> {
            repo.setUrl("Internal minecraft");
            repo.setUrl(minecraftRepository.getBaseDir());
        });
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
}
