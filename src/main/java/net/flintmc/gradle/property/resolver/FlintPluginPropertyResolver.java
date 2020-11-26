package net.flintmc.gradle.property.resolver;

import net.flintmc.gradle.property.FlintPluginProperty;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for property value resolvers.
 *
 * @param <T> The type of the property value
 */
public abstract class FlintPluginPropertyResolver<T> {
  private static final Logger LOGGER = Logging.getLogger(FlintPluginPropertyResolver.class);
  private static final Map<Class<?>, FlintPluginPropertyResolver<?>> RESOLVERS = new HashMap<>();

  /**
   * Retrieves the property resolver for the given type.
   *
   * @param type The type to retrieve the resolver for
   * @param <T>  The type of the resolver
   * @return The resolver
   * @throws IllegalArgumentException If no resolver for the given type is registered
   */
  @SuppressWarnings("unchecked") // manually type checked
  public static <T> FlintPluginPropertyResolver<T> forType(Class<T> type) {
    if(RESOLVERS.isEmpty()) {
      RESOLVERS.put(boolean.class, new BooleanPropertyResolver());
      RESOLVERS.put(String.class, new StringPropertyResolver());
      RESOLVERS.put(URI.class, new URIPropertyResolver());
    }

    FlintPluginPropertyResolver<T> resolver = ((FlintPluginPropertyResolver<T>) RESOLVERS.get(type));
    if(resolver == null) {
      throw new IllegalArgumentException("No resolver registered for type " + type.getName());
    }

    return resolver;
  }

  /**
   * Resolves the property for the given project instance.
   * <p>
   * The resolver uses the following steps to resolve the property:
   * <ol>
   *   <li>{@link FlintPluginProperty#getEnvironmentName()} - From the system environment</li>
   *   <li>{@link FlintPluginProperty#getPropertyName()} - From the project properties</li>
   *   <li>{@link FlintPluginProperty#getDeprecatedEnvironmentNames()} - From the system environment</li>
   *   <li>{@link FlintPluginProperty#getDeprecatedNames()} - From the project properties</li>
   *   <li>{@code null} as a last resort</li>
   * </ol>
   * The first step to succeed determines the value.
   *
   * @param property The property to resolve
   * @param project  The project to resolve the property for
   * @return The resolved value, or {@code null}, if the property was not found
   * @throws IllegalArgumentException If the value of the property can not be parsed
   */
  public abstract T resolve(FlintPluginProperty<T> property, Project project);

  /**
   * Tries to resolve a raw property value as specified by {@link #resolve(FlintPluginProperty, Project)}.
   *
   * @param property The property to resolve
   * @param project  The project to resolve the property on
   * @return The resolved value, or {@code null} if the value could not be resolved
   */
  protected static String resolveRaw(FlintPluginProperty<?> property, Project project) {
    LOGGER.trace("Resolving property {} for project {}", property.getPropertyName(), project.getDisplayName());

    String environmentName = property.getEnvironmentName();
    String propertyName = property.getPropertyName();

    LOGGER.debug(
        "Trying to resolve property {} for project {} using environment variable {}",
        propertyName,
        project.getDisplayName(),
        environmentName
    );

    String environmentValue = System.getenv(environmentName);
    if(environmentValue != null) {
      LOGGER.debug(
          "Resolved property {} for project {} using the system environment to value {}",
          propertyName,
          project.getDisplayName(),
          environmentValue
      );

      return environmentValue;
    }

    LOGGER.debug(
        "Trying to resolve property {} for project {} using its name",
        propertyName,
        project.getDisplayName()
    );
    Object propertyValue = project.getProperties().get(propertyName);

    if(propertyValue != null) {
      String value = propertyValue.toString();
      LOGGER.debug(
          "Resolved property {} for project {} using its name to value {}",
          propertyName,
          project.getDisplayName(),
          value
      );

      return value;
    }

    LOGGER.trace(
        "Failed to resolve property {} for project {}, checking deprecated environment names",
        propertyName,
        project.getDisplayName()
    );

    for(String deprecatedEnvironmentName : property.getDeprecatedEnvironmentNames()) {
      LOGGER.debug(
          "Trying to resolve property {} for project {} using deprecated environment variable {}",
          deprecatedEnvironmentName,
          project.getDisplayName(),
          environmentName
      );

      String deprecatedEnvironmentValue = System.getenv(deprecatedEnvironmentName);
      if(deprecatedEnvironmentValue != null) {
        LOGGER.debug(
            "Resolved property {} for project {} using the deprecated system environment to value {}",
            propertyName,
            project.getDisplayName(),
            deprecatedEnvironmentValue
        );
        LOGGER.warn(
            "The environment variable {} is deprecated, use {} instead!", deprecatedEnvironmentName, environmentName);

        return deprecatedEnvironmentValue;
      }
    }

    LOGGER.trace(
        "Failed to resolve property {} for project {}, checking deprecated property names",
        propertyName,
        project.getDisplayName()
    );

    for(String deprecatedPropertyName : property.getDeprecatedNames()) {
      LOGGER.debug(
          "Trying to resolve property {} for project {} using its deprecated name {}",
          propertyName,
          project.getDisplayName(),
          deprecatedPropertyName
      );
      Object deprecatedPropertyValue = project.getProperties().get(deprecatedPropertyName);

      if(deprecatedPropertyValue != null) {
        String value = deprecatedPropertyValue.toString();
        LOGGER.debug(
            "Resolved property {} for project {} using its deprecated name to value {}",
            propertyName,
            project.getDisplayName(),
            value
        );
        LOGGER.warn("The property name {} is deprecated, use {} instead", deprecatedPropertyName, propertyName);

        return value;
      }
    }

    LOGGER.trace("Failed to resolve property {} by any means, returning null", property.getPropertyName());
    return null;
  }

  /**
   * Tells the user that a required property is missing and briefs him about possible solutions, then throws an {@link
   * IllegalStateException}.
   *
   * @param project        The project the property is missing from
   * @param property       The property that is missing
   * @param otherSolutions Other solution that should be presented to the user
   * @throws IllegalStateException Always
   */
  public static void abortPropertyMissing(Project project, FlintPluginProperty<?> property, String... otherSolutions) {
    LOGGER.error("The property {} is missing!", property.getPropertyName());
    LOGGER.error("The following solutions are available:");
    LOGGER.error("- Add the property to the project gradle.properties next to the build.gradle");
    LOGGER.error("- Add the property to {}",
        new File(project.getGradle().getGradleUserHomeDir(), "gradle.properties").getAbsolutePath());
    LOGGER.error("- Set the environment variable {}", property.getEnvironmentName());

    for(String solution : otherSolutions) {
      LOGGER.error("- {}", solution);
    }

    throw new IllegalStateException(
        "Missing required property " + property.getPropertyName() + " on " + project.getDisplayName());
  }
}
