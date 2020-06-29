package net.labyfy.gradle.environment;

import net.labyfy.gradle.environment.mcp.ModCoderPackEnvironment;
import net.labyfy.gradle.minecraft.data.environment.EnvironmentInput;

import java.util.Collections;
import java.util.List;

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
     * Retrieves the classifier base name for jars produced by this environment.
     *
     * @return The classifier base name
     */
    String classifierName();

    static List<DeobfuscationEnvironment> createFor(EnvironmentInput input) {
        return Collections.singletonList(new ModCoderPackEnvironment(input.getModCoderPack()));
    }
}
