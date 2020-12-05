package net.flintmc.gradle.property.resolver;

import net.flintmc.gradle.property.FlintPluginProperty;
import org.gradle.api.Project;

/**
 * Resolver for strings.
 */
public class StringPropertyResolver extends FlintPluginPropertyResolver<String> {
  @Override
  public String resolve(FlintPluginProperty<String> property, Project project) {
    return resolveRaw(property, project);
  }
}
