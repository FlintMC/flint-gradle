package net.labyfy.gradle.environment;

import net.labyfy.gradle.environment.mcp.ModCoderPackEnvironment;
import net.labyfy.gradle.maven.pom.MavenArtifact;
import net.labyfy.gradle.maven.pom.MavenPom;
import net.labyfy.gradle.minecraft.data.environment.EnvironmentInput;

import java.util.Collection;

/**
 * Represents a deobfuscation environment which can be used to obtain the minecraft source.
 */
public interface DeobfuscationEnvironment {
    /**
     * Retrieves the name of the environment. Needs to be filename friendly.
     *
     * @return The name of the environment
     */
    String name();

    /**
     * Runs the deobfuscation on the given client and server artifacts. One of the 2 artifacts may
     * be null, but never both at the same time.
     *
     * @param clientPom The client artifact, may be null if serverPom is not null
     * @param serverPom The client artifact, may be null if clientPom is not null
     * @param utilities Various utilities useful during deobfuscation
     */
    void runDeobfuscation(MavenPom clientPom, MavenPom serverPom, DeobfuscationUtilities utilities)
            throws DeobfuscationException;

    /**
     * Retrieves a collection of all artifacts which should be added to the compile classpath.
     *
     * @param client The client artifact, may be null
     * @param server The server artifact, may be null
     * @return A collection of artifacts which should be added to the compile classpath
     */
    Collection<MavenArtifact> getCompileArtifacts(MavenArtifact client, MavenArtifact server);

    /**
     * Retrieves a collection of all artifacts which should be added to the runtime classpath.
     *
     * @param client The client artifact, may be null
     * @param server The server artifact, may be null
     * @return A collection of artifacts which should be added to the runtime classpath
     */
    Collection<MavenArtifact> getRuntimeArtifacts(MavenArtifact client, MavenArtifact server);

    static DeobfuscationEnvironment createFor(EnvironmentInput input) {
        return new ModCoderPackEnvironment(input.getModCoderPack());
    }
}
