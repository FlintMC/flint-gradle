package net.labyfy.gradle.minecraft.data.version;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonDeserialize(using = VersionedLibraryName.Deserializer.class)
public class VersionedLibraryName {
    private String group;
    private String name;
    private String version;
    private String classifier;
    private String extension;

    public VersionedLibraryName(String group, String name, String version, String classifier, String extension) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
        this.extension = extension;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VersionedLibraryName that = (VersionedLibraryName) o;
        return Objects.equals(group, that.group) &&
                Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(classifier, that.classifier) &&
                Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name, version, classifier, extension);
    }

    public static class Deserializer extends StdScalarDeserializer<VersionedLibraryName> {
        public Deserializer() {
            super(String.class);
        }

        @Override
        public VersionedLibraryName deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = StringDeserializer.instance.deserialize(p, ctxt);
            String[] parts = value.split(":", 4);

            // group:name:version:classifier@extension
            String group = parts[0];
            String name = parts[1];
            String version = parts[2];

            String classifier = null;
            String extension = null;

            if(parts.length > 3) {
                String remaining = parts[3];
                int indexOfAt = remaining.lastIndexOf('@');

                if(indexOfAt >= 0) {
                    classifier = remaining.substring(0, indexOfAt);
                    extension = remaining.substring(indexOfAt + 1);
                } else {
                    classifier = remaining;
                    extension = null;
                }
            }

            return new VersionedLibraryName(group, name, version, classifier, extension);
        }
    }
}
