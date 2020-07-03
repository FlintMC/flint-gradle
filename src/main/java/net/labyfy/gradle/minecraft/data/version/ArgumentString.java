package net.labyfy.gradle.minecraft.data.version;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import net.labyfy.gradle.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonDeserialize(using = ArgumentString.Deserializer.class)
public class ArgumentString {
    private final String variableName;
    private final String value;
    private final List<VersionedRule> rules;

    private ArgumentString(String variableName, String value, List<VersionedRule> rules) {
        this.variableName = variableName;
        this.value = value;
        this.rules = rules;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getValue() {
        return value;
    }

    public List<VersionedRule> getRules() {
        return rules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgumentString that = (ArgumentString) o;
        return Objects.equals(variableName, that.variableName) &&
                Objects.equals(value, that.value) &&
                Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableName, value, rules);
    }

    public static class Deserializer extends StdNodeBasedDeserializer<ArgumentString> {
        protected Deserializer() {
            super(ArgumentString.class);
        }

        @Override
        public ArgumentString convert(JsonNode root, DeserializationContext ctxt) throws IOException {
            if (root.isTextual()) {
                String value = root.asText();
                return new ArgumentString(getVariableName(value), getNonVariableText(value), null);
            } else {
                String value = root.get("value").requireNonNull().asText();
                JavaType ruleListType =
                        ctxt.getTypeFactory().constructCollectionType(ArrayList.class, VersionedRule.class);

                return new ArgumentString(getVariableName(value), getNonVariableText(value),
                        Util.readJsonValue(ruleListType, root.get("rules"), ctxt));
            }
        }

        private String getNonVariableText(String text) {
            int indexOfVarStart = text.indexOf("${");
            if (indexOfVarStart == -1) {
                return text;
            }

            return text.substring(0, indexOfVarStart);
        }

        private String getVariableName(String text) {
            int indexOfVarStart = text.indexOf("${");
            if (indexOfVarStart == -1) {
                return null;
            }

            int indexOfVarEnd = text.indexOf('}', indexOfVarStart);
            return text.substring(indexOfVarStart + 2, indexOfVarEnd);
        }
    }
}
