package net.flintmc.gradle.minecraft.data.version;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import net.flintmc.gradle.util.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public static class ListDeserializer extends JsonDeserializer<List<ArgumentString>> {
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

        @Override
        public List<ArgumentString> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode root = p.getCodec().readTree(p);
            if (!root.isArray()) {
                throw JsonMappingException.from(p, "Arguments is not an array");
            }
            List<ArgumentString> outputs = new ArrayList<>();

            for (int i = 0; i < root.size(); i++) {
                JsonNode argument = root.get(i);

                if (argument.isTextual()) {
                    String value = argument.asText();
                    outputs.add(new ArgumentString(getVariableName(value), getNonVariableText(value), null));
                } else {
                    JsonNode valueNode = argument.get("value").requireNonNull();
                    JavaType ruleListType =
                        ctxt.getTypeFactory().constructCollectionType(ArrayList.class, VersionedRule.class);

                    if (valueNode.isTextual()) {
                        String value = valueNode.asText();
                        outputs.add(new ArgumentString(getVariableName(value), getNonVariableText(value),
                            Util.readJsonValue(ruleListType, argument.get("rules"), ctxt)));
                    } else if (valueNode.isArray()) {
                        for (int j = 0; j < valueNode.size(); j++) {
                            String value = valueNode.get(j).requireNonNull().asText();
                            outputs.add(new ArgumentString(getVariableName(value), getNonVariableText(value),
                                Util.readJsonValue(ruleListType, argument.get("rules"), ctxt)));
                        }
                    } else {
                        throw JsonMappingException.from(p,
                            "Value of argument string with rules is neither a string nor an array");
                    }
                }
            }

            return outputs;
        }
    }
}
