package net.flintmc.gradle.property;

import java.net.URI;

/**
 * Global list of all {@link FlintPluginProperty} instances.
 */
public final class FlintPluginProperties {
  /**
   * Determines whether the minecraft run task should prompt for login.
   * <p>
   * The property is a boolean and can be set using the project property {@code net.flint,c.enable-minecraft-login} or
   * the environment variable {@code FLINT_ENABLE_MINECRAFT_LOGIN}.
   * <p>
   * The deprecated property names are:
   * <ul>
   *   <li>{@code net.labyfy.gradle.login} - Uses the old Labyfy name</li>
   *   <li>{@code net.flint.gradle.login} - Ambiguous name</li>
   *   <li>{@code net.flint.enable-minecraft-login} - Not using net.flintmc</li>
   * </ul>
   */
  public static final FlintPluginProperty<Boolean> ENABLE_MINECRAFT_LOGIN = FlintPluginProperty.builder()
      .name("net.flintmc.enable-minecraft-login")
      .environment("FLINT_ENABLE_MINECRAFT_LOGIN")
      .deprecatedName("net.labyfy.gradle.login")
      .deprecatedName("net.flint.gradle.login")
      .deprecatedName("net.flint.enable-minecraft-login")
      .complete(boolean.class, false);

  /**
   * Determines the URL of the flint distributor.
   * <p>
   * The property is a URL and can be set using the project property {@code net.flintmc.distributor.url} or the
   * environment variable {@code FLINT_DISTRIBUTOR_URL}.
   *
   * The deprecated property names are:
   * <ul>
   *   <li>{@code net.flint.distributor.url} - Not using net.flintmc</li>
   * </ul>
   */
  public static final FlintPluginProperty<URI> DISTRIBUTOR_URL = FlintPluginProperty.builder()
      .name("net.flintmc.distributor.url")
      .environment("FLINT_DISTRIBUTOR_URL")
      .deprecatedName("net.flint.distributor.url")
      .complete(URI.class, URI.create("https://dist.labymod.net"));

  /**
   * Determines the release channel of the flint distributor.
   * <p>
   * The property is a string and can be set using the project property {@code net.flintmc.distributor.channel} or the
   * environment variable {@code FLINT_DISTRIBUTOR_CHANNEL}. The default value is {@code release}.
   *
   * The deprecated property names are:
   * <ul>
   *   <li>{@code net.flint.distributor.channel} - Not using net.flintmc</li>
   * </ul>
   */
  public static final FlintPluginProperty<String> DISTRIBUTOR_CHANNEL = FlintPluginProperty.builder()
      .name("net.flintmc.distributor.channel")
      .environment("FLINT_DISTRIBUTOR_CHANNEL")
      .deprecatedName("net.flint.distributor.channel")
      .complete(String.class, "release");

  /**
   * Determines the bearer token used for authorization with the flint distributor. You usually want the {@link
   * #DISTRIBUTOR_PUBLISH_TOKEN} as a user, as the bearer token is used for administrative access.
   * <p>
   * The property is a string and can be set using the project property {@code net.flintmc.distributor.bearer-token} or
   * the environment variable {@code FLINT_DISTRIBUTOR_BEARER_TOKEN}.
   * <p>
   * The deprecated property names are:
   * <ul>
   *   <li>{@code net.flint.distributor.bearer-token} - Not using net.flintmc</li>
   * </ul>
   * <p>
   * The deprecated environment variables are:
   * <ul>
   *   <li>{@code FLINT_DISTRIBUTOR_AUTHORIZATION} - Ambiguous name</li>
   * </ul>
   */
  public static final FlintPluginProperty<String> DISTRIBUTOR_BEARER_TOKEN = FlintPluginProperty.builder()
      .name("net.flintmc.distributor.bearer-token")
      .deprecatedName("net.flint.distributor.bearer-token")
      .environment("FLINT_DISTRIBUTOR_BEARER_TOKEN")
      .deprecatedEnvironment("FLINT_DISTRIBUTOR_AUTHORIZATION")
      .complete(String.class);

  /**
   * Determines the publish token used for publish authorization with the flint distributor.
   * <p>
   * The property is a string and can be set using the project property {@code net.flint,c.distributor.publish-token} or
   * the environment variable {@code FLINT_DISTRIBUTOR_PUBLISH_TOKEN}.
   * <p>
   * The deprecated property names are:
   * <ul>
   *   <li>{@code net.flint.distributor.publish-token} - Not using net.flintmc</li>
   * </ul>
   */
  public static final FlintPluginProperty<String> DISTRIBUTOR_PUBLISH_TOKEN = FlintPluginProperty.builder()
      .name("net.flintmc.distributor.publish-token")
      .deprecatedName("net.flint.distributor.publish-token")
      .environment("FLINT_DISTRIBUTOR_PUBLISH_TOKEN")
      .complete(String.class);
}
