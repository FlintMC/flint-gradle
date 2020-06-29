package net.labyfy.gradle.minecraft.data.version;

import java.util.Map;
import java.util.Objects;

public class VersionedRule {
    private VersionedRuleAction action;
    private Map<String, Object> features;
    private VersionedOsRule os;

    public VersionedRuleAction getAction() {
        return action;
    }

    public Map<String, Object> getFeatures() {
        return features;
    }

    public VersionedOsRule getOs() {
        return os;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionedRule that = (VersionedRule) o;
        return action == that.action &&
                Objects.equals(features, that.features) &&
                Objects.equals(os, that.os);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, features, os);
    }
}
