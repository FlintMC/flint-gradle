package net.flintmc.gradle.property.resolver;

import net.flintmc.gradle.property.FlintPluginProperty;
import org.gradle.api.Project;

/**
 * Resolver for boolean values.
 */
public class BooleanPropertyResolver extends FlintPluginPropertyResolver<Boolean> {
  @Override
  public Boolean resolve(FlintPluginProperty<Boolean> property, Project project) {
    String value = resolveRaw(property, project);
    if(value == null) {
      return null;
    }

    switch(value.toLowerCase()) {
      case "yes":
      case "y":
      case "1":
      case "true":
        return true;

      case "no":
      case "n":
      case "0":
      case "false":
        return false;

      default:
        throw new IllegalArgumentException("Failed to parse " + value + " as a boolean, use true or false");
    }
  }
}
