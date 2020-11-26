package net.flintmc.gradle.property;

import java.net.URL;

/**
 * Global list of all {@link FlintPluginProperty} instances.
 */
public final class FlintPluginProperties {
  /**
   * Determines whether the minecraft run task should prompt for login.
   * <p>
   * The property is a boolean and can be set using the project property {@code net.flint.enable-minecraft-login} or the
   * environment variable {@code FLINT_ENABLE_MINECRAFT_LOGIN}.
   * <p>
   * The deprecated property names are:
   * <ul>
   *   <li>{@code net.labyfy.gradle.login} - Uses the old Labyfy name</li>
   *   <li>{@code net.flint.gradle.login} - Ambiguous name</li>
   * </ul>
   */
  public static final FlintPluginProperty<Boolean> ENABLE_MINECRAFT_LOGIN = FlintPluginProperty.builder()
      .name("net.flint.enable-minecraft-login")
      .environment("FLINT_ENABLE_MINECRAFT_LOGIN")
      .deprecatedName("net.labyfy.gradle.login")
      .deprecatedName("net.flint.gradle.login")
      .complete(boolean.class, false);

  /**
   * Determines the URL of the flint distributor.
   * <p>
   * The property is a URL and can be set using the project property {@code net.flint.distributor.url} or the
   * environment variable {@code FLINT_DISTRIBUTOR_URL}.
   */
  public static final FlintPluginProperty<URL> DISTRIBUTOR_URL = FlintPluginProperty.builder()
      .name("net.flint.distributor.url")
      .environment("FLINT_DISTRIBUTOR_URL")
      .complete(URL.class);

  /**
   * Determines the release channel of the flint distributor.
   * <p>
   * The property is a string and can be set using the project property {@code net.flint.distributor.channel} or the
   * environment variable {@code FLINT_DISTRIBUTOR_CHANNEL}. The default value is {@code release}.
   */
  public static final FlintPluginProperty<String> DISTRIBUTOR_CHANNEL = FlintPluginProperty.builder()
      .name("net.flint.distributor.channel")
      .environment("FLINT_DISTRIBUTOR_CHANNEL")
      .complete(String.class, "release");

  /**
   * Determines the bearer token used for authorization with the flint distributor. You usually want the {@link
   * #DISTRIBUTOR_PUBLISH_TOKEN} as a user, as the bearer token is used for administrative access.
   * <p>
   * The property is a string and can be set using the project property {@code net.flint.distributor.bearer-token} or
   * the environment variable {@code FLINT_DISTRIBUTOR_BEARER_TOKEN}.
   * <p>
   * The deprecated environment variables are:
   * <ul>
   *   <li>{@code FLINT_DISTRIBUTOR_AUTHORIZATION} - Ambiguous name</li>
   * </ul>
   */
  public static final FlintPluginProperty<String> DISTRIBUTOR_BEARER_TOKEN = FlintPluginProperty.builder()
      .name("net.flint.distributor.bearer-token")
      .environment("FLINT_DISTRIBUTOR_BEARER_TOKEN")
      .deprecatedEnvironment("FLINT_DISTRIBUTOR_AUTHORIZATION")
      .complete(String.class);

  /**
   * Determines the publish token used for publish authorization with the flint distributor.
   * <p>
   * The property is a string and can be set using the project property {@code net.flint.distributor.publish-token} or
   * the environment variable {@code FLINT_DISTRIBUTOR_PUBLISH_TOKEN}.
   */
  public static final FlintPluginProperty<String> DISTRIBUTOR_PUBLISH_TOKEN = FlintPluginProperty.builder()
      .name("net.flint.distributor.publish-token")
      .environment("FLINT_DISTRIBUTOR_PUBLISH_TOKEN")
      .complete(String.class);
}
