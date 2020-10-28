package net.flintmc.gradle.minecraft.data.version;

import java.util.Objects;

public class VersionedOsRule {
    private VersionedOsRuleValue name;
    private String version;
    private String arch;

    public VersionedOsRuleValue getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getArch() {
        return arch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionedOsRule that = (VersionedOsRule) o;
        return name == that.name &&
            Objects.equals(version, that.version) &&
            Objects.equals(arch, that.arch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, arch);
    }
}
