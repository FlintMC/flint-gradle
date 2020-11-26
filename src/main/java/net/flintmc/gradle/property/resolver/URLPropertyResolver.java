package net.flintmc.gradle.property.resolver;

import net.flintmc.gradle.property.FlintPluginProperty;
import org.gradle.api.Project;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Resolver for URL properties.
 */
public class URLPropertyResolver extends FlintPluginPropertyResolver<URL> {
  @Override
  public URL resolve(FlintPluginProperty<URL> property, Project project) {
    String value = resolveRaw(property, project);
    if(value == null) {
      return null;
    }

    try {
      return new URL(value);
    } catch(MalformedURLException e) {
      throw new IllegalArgumentException("Failed to parse URL", e);
    }
  }
}
