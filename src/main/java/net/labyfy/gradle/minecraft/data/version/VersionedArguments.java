package net.labyfy.gradle.minecraft.data.version;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Objects;

public class VersionedArguments {
    @JsonDeserialize(using = ArgumentString.ListDeserializer.class)
    private List<ArgumentString> game;

    @JsonDeserialize(using = ArgumentString.ListDeserializer.class)
    private List<ArgumentString> jvm;

    public List<ArgumentString> getGame() {
        return game;
    }

    public List<ArgumentString> getJvm() {
        return jvm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionedArguments that = (VersionedArguments) o;
        return Objects.equals(game, that.game) &&
                Objects.equals(jvm, that.jvm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(game, jvm);
    }
}
