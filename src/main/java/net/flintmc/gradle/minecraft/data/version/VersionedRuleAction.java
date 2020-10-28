package net.flintmc.gradle.minecraft.data.version;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum VersionedRuleAction {
    @JsonProperty("allow") ALLOW,
    @JsonProperty("disallow") DISALLOW
}
