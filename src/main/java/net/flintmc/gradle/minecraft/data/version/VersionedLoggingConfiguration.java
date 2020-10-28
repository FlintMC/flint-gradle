package net.flintmc.gradle.minecraft.data.version;

import java.util.Objects;

public class VersionedLoggingConfiguration {
  private String argument;
  private VersionedArtifactDownload file;
  private VersionedLoggingConfigurationType type;

  public String getArgument() {
    return argument;
  }

  public VersionedArtifactDownload getFile() {
    return file;
  }

  public VersionedLoggingConfigurationType getType() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VersionedLoggingConfiguration that = (VersionedLoggingConfiguration) o;
    return Objects.equals(argument, that.argument) &&
        Objects.equals(file, that.file) &&
        type == that.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(argument, file, type);
  }
}
