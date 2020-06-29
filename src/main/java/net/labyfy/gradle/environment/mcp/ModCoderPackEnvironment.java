package net.labyfy.gradle.environment.mcp;

import net.labyfy.gradle.environment.DeobfuscationEnvironment;
import net.labyfy.gradle.minecraft.data.environment.ModCoderPackInput;

public class ModCoderPackEnvironment implements DeobfuscationEnvironment {
    private final ModCoderPackInput input;

    public ModCoderPackEnvironment(ModCoderPackInput input) {
        this.input = input;
    }

    @Override
    public String name() {
        return "ModCoderPack";
    }

    @Override
    public String classifierName() {
        return "mcp-" + input.getConfigVersion() + "-" + input.getMappingsVersion();
    }
}
