package net.flintmc.gradle.minecraft.yggdrasil;

/**
 * Exception indicating that authentication failed.
 */
public class YggdrasilAuthenticationException extends Exception {
  /**
   * Constructs a new {@link YggdrasilAuthenticationException} with the given message.
   *
   * @param message The message explaining why the authentication failed
   */
  public YggdrasilAuthenticationException(String message) {
    super(message);
  }

  /**
   * Constructs a new {@link YggdrasilAuthenticationException} with the given message and cause.
   *
   * @param message The message explaining why authentication failed
   * @param cause   The cause of this exception
   */
  public YggdrasilAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
