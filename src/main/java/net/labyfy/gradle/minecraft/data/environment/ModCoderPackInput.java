package net.labyfy.gradle.minecraft.data.environment;

import java.net.URL;

public class ModCoderPackInput {
    private String configVersion;
    private URL configDownload;
    private String mappingsVersion;
    private URL mappingsDownload;

    public String getConfigVersion() {
        return configVersion;
    }

    public URL getConfigDownload() {
        return configDownload;
    }

    public String getMappingsVersion() {
        return mappingsVersion;
    }

    public URL getMappingsDownload() {
        return mappingsDownload;
    }
}
