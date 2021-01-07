package net.flintmc.gradle.minecraft.data.environment;

public class MinecraftVersion {

  private final String version;
  private final EnvironmentType environmentType;

  public MinecraftVersion(String version, EnvironmentType environmentType) {
    this.version = version;
    this.environmentType = environmentType;
  }

  public String getVersion() {
    return version;
  }

  public EnvironmentType getEnvironmentType() {
    return environmentType;
  }
}
