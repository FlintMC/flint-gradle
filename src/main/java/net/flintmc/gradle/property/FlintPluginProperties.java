package net.flintmc.gradle.property;

/**
 * Global list of all {@link FlintPluginProperty} instances.
 */
public final class FlintPluginProperties {
  /**
   * Determines whether the minecraft run task should prompt for login.
   * <p>
   * The property is a boolean and can be accessed using the project property
   * {@code net.flint.gradle.enable-minecraft-login} or the environment variable {@code FLINT_ENABLE_MINECRAFT_LOGIN}.
   * <p>
   * The deprecated property names are:
   * <ul>
   *   <li>{@code net.labyfy.gradle.login} - Uses the old Labyfy name</li>
   *   <li>{@code net.flint.gradle.login} - Ambiguous name</li>
   * </ul>
   */
  public static final FlintPluginProperty<Boolean> ENABLE_MINECRAFT_LOGIN = FlintPluginProperty.builder()
      .name("net.flint.gradle.enable-minecraft-login")
      .environment("FLINT_ENABLE_MINECRAFT_LOGIN")
      .deprecatedName("net.labyfy.gradle.login")
      .deprecatedName("net.flint.gradle.login")
      .complete(boolean.class, false);
}
