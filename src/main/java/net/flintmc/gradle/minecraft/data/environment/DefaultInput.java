package net.flintmc.gradle.minecraft.data.environment;

import java.net.URL;

public abstract class DefaultInput {

  private String configVersion;
  private URL configDownload;
  private String mappingsVersion;
  private URL mappingsDownload;

  /**
   * Retrieves the config version of this input.
   *
   * @return The config version of this input.
   */
  public String getConfigVersion() {
    return this.configVersion;
  }

  /**
   * Retrieves the download url where the config is located.
   *
   * @return The download url where the config is located.
   */
  public URL getConfigDownload() {
    return this.configDownload;
  }

  /**
   * Retrieves the mappings version of this input.
   *
   * @return The mappings version of this input.
   */
  public String getMappingsVersion() {
    return this.mappingsVersion;
  }

  /**
   * Retrieves the download url where the mappings are located.
   *
   * @return The download url where the mappings are located.
   */
  public URL getMappingsDownload() {
    return this.mappingsDownload;
  }
}
