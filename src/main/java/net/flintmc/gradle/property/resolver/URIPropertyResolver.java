package net.flintmc.gradle.property.resolver;

import net.flintmc.gradle.property.FlintPluginProperty;
import org.gradle.api.Project;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Resolver for URI/URL properties.
 */
public class URIPropertyResolver extends FlintPluginPropertyResolver<URI> {
  @Override
  public URI resolve(FlintPluginProperty<URI> property, Project project) {
    String value = resolveRaw(property, project);
    if(value == null) {
      return null;
    }

    try {
      return new URI(value);
    } catch(URISyntaxException e) {
      throw new IllegalArgumentException("Failed to parse URI", e);
    }
  }
}
