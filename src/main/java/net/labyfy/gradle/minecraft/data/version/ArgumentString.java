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
    private String value;
    private boolean isVariable;
    private List<VersionedRule> rules;

    private ArgumentString(String value, boolean isVariable, List<VersionedRule> rules) {
        this.value = value;
        this.isVariable = isVariable;
        this.rules = rules;
    }

    public String getValue() {
        return value;
    }

    public boolean isVariable() {
        return isVariable;
    }

    public List<VersionedRule> getRules() {
        return rules;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgumentString that = (ArgumentString) o;
        return isVariable == that.isVariable &&
                Objects.equals(value, that.value) &&
                Objects.equals(rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, isVariable, rules);
    }

    public static class Deserializer extends StdNodeBasedDeserializer<ArgumentString> {
        protected Deserializer() {
            super(ArgumentString.class);
        }

        @Override
        public ArgumentString convert(JsonNode root, DeserializationContext ctxt) throws IOException {
            if(root.isTextual()) {
                String value = root.asText();
                return new ArgumentString(asNonVariableText(value), isVariable(value), null);
            } else {
                String value = root.get("value").requireNonNull().asText();
                JavaType ruleListType =
                        ctxt.getTypeFactory().constructCollectionType(ArrayList.class, VersionedRule.class);

                return new ArgumentString(asNonVariableText(value), isVariable(value),
                        Util.readJsonValue(ruleListType, root.get("rules"), ctxt));
            }
        }

        private boolean isVariable(String input) {
            return input.startsWith("$");
        }

        private String asNonVariableText(String text) {
            if(!text.startsWith("$")) {
                return text;
            } else {
                String newText = text.substring(2);
                return newText.substring(0, newText.length() - 1);
            }
        }
    }
}
