package net.flintmc.gradle.minecraft.data.version;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum VersionedOsRuleValue {
  @JsonProperty("linux") LINUX,
  @JsonProperty("windows") WINDOWS,
  @JsonProperty("osx") OSX,
  OTHER
}
