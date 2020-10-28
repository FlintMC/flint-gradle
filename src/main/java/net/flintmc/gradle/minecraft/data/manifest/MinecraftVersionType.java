package net.flintmc.gradle.minecraft.data.manifest;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MinecraftVersionType {
  @JsonProperty("release") RELEASE,
  @JsonProperty("snapshot") SNAPSHOT,
  @JsonProperty("beta") BETA,
  @JsonProperty("old_beta") OLD_BETA,
  @JsonProperty("alpha") ALPHA,
  @JsonProperty("old_alpha") OLD_ALPHA
}
