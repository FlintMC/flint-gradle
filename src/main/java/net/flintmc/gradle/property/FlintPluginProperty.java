package net.flintmc.gradle.property;

import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.property.resolver.FlintPluginPropertyResolver;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Property that the plugin may access in one way or another.
 */
public final class FlintPluginProperty<T> {
  private final String propertyName;
  private final String environmentName;

  private final List<String> deprecatedNames;
  private final List<String> deprecatedEnvironmentNames;

  private final FlintPluginPropertyResolver<T> resolver;
  private final T defaultValue;

  private FlintPluginProperty(
      String propertyName,
      String environmentName,
      List<String> deprecatedNames,
      List<String> deprecatedEnvironmentNames,
      FlintPluginPropertyResolver<T> resolver,
      T defaultValue
  ) {
    this.propertyName = propertyName;
    this.environmentName = environmentName;
    this.deprecatedNames = deprecatedNames;
    this.deprecatedEnvironmentNames = deprecatedEnvironmentNames;
    this.resolver = resolver;
    this.defaultValue = defaultValue;
  }

  /**
   * Retrieves the name of the property when read from the project properties.
   *
   * @return The name of the property for the project properties
   */
  public String getPropertyName() {
    return "net.flint.gradle." + propertyName;
  }

  /**
   * Retrieves the name of the property when read from the system environment.
   *
   * @return The name of the property for the system environment
   */
  public String getEnvironmentName() {
    return environmentName;
  }

  /**
   * Retrieves a list of names which have been deprecated when reading the property from the project properties.
   *
   * @return A list of deprecated property names for the project properties
   */
  public List<String> getDeprecatedNames() {
    return deprecatedNames;
  }

  /**
   * Retrieves a list of names which have been deprecated when reading the property from the system environment.
   *
   * @return A list of deprecated property names for the system environment
   */
  public List<String> getDeprecatedEnvironmentNames() {
    return deprecatedEnvironmentNames;
  }

  /**
   * Resolves the property value for the given plugin.
   *
   * @param plugin The plugin to resolve the property for
   * @return The resolved value, or the default, if the value could not be resolved
   * @throws IllegalArgumentException If the value of the property can not be parsed
   */
  public T resolve(FlintGradlePlugin plugin) {
    return resolve(plugin.getProject());
  }

  /**
   * Resolves the property value for the given project.
   *
   * @param project The project to resolve the property for
   * @return The resolved value, or the default, if the value could not be resolved
   * @throws IllegalArgumentException If the value of the property can not be parsed
   */
  public T resolve(Project project) {
    T value = resolver.resolve(this, project);
    return value == null ? defaultValue : value;
  }

  /**
   * Creates a new property builder.
   *
   * @return The created builder
   */
  static Builder builder() {
    return new Builder();
  }

  /**
   * Internal builder for plugin properties.
   */
  static class Builder {
    private String propertyName;
    private String environmentName;
    private List<String> deprecatedNames;
    private List<String> deprecatedEnvironmentNames;

    /**
     * Sets the project properties name of the property.
     *
     * @param name The name of the property
     * @return this
     * @throws IllegalStateException If the name is set already
     */
    public Builder name(String name) {
      if(propertyName != null) {
        throw new IllegalStateException("Property name already set to " + name);
      }

      propertyName = name;
      return this;
    }

    /**
     * Sets the environment name of the property.
     *
     * @param name The environment name of the property
     * @return this
     * @throws IllegalStateException If the environment name is set already
     */
    public Builder environment(String name) {
      if(environmentName != null) {
        throw new IllegalStateException("Environment name already set to " + name);
      }

      environmentName = name;
      return this;
    }

    /**
     * Adds a deprecated project properties name to the property.
     *
     * @param name The deprecated project properties name
     * @return this
     */
    public Builder deprecatedName(String name) {
      if(deprecatedNames == null) {
        deprecatedNames = new ArrayList<>();
      }

      deprecatedNames.add(name);
      return this;
    }

    /**
     * Adds a deprecated environment name to the property.
     *
     * @param name The deprecated environment name
     * @return this
     */
    public Builder deprecatedEnvironment(String name) {
      if(deprecatedEnvironmentNames == null) {
        deprecatedEnvironmentNames = new ArrayList<>();
      }

      deprecatedEnvironmentNames.add(name);
      return this;
    }

    /**
     * Completes building the property for the given type.
     *
     * @param type The type of the property
     * @param <T>  The type of the property
     * @return The built property
     * @throws IllegalStateException If properties are missing or have invalid values
     */
    public <T> FlintPluginProperty<T> complete(Class<T> type) {
      return complete(type, null);
    }

    /**
     * Completes building the property for the given type.
     *
     * @param type         The type of the property
     * @param defaultValue The default value of the property
     * @param <T>          The type of the property
     * @return The built property
     * @throws IllegalStateException If fields are missing or have invalid values
     */
    public <T> FlintPluginProperty<T> complete(Class<T> type, T defaultValue) {
      if(propertyName == null) {
        throw new IllegalStateException("propertyName can not be null");
      } else if(environmentName == null) {
        throw new IllegalStateException("environmentName can not be null");
      }

      return new FlintPluginProperty<>(
          propertyName,
          environmentName,
          deprecatedNames == null ? Collections.emptyList() : deprecatedNames,
          deprecatedEnvironmentNames == null ? Collections.emptyList() : deprecatedEnvironmentNames,
          FlintPluginPropertyResolver.forType(type),
          defaultValue
      );
    }
  }
}
