package net.flintmc.gradle.minecraft.ui;

/**
 * Represents the action a user has chosen in the login dialog
 */
public enum LoginDialogResult {
  /**
   * The user decided to login
   */
  ATTEMPT_LOGIN,

  /**
   * The user decided to abort the launch
   */
  ABORT,

  /**
   * The user decided to continue the launch offline
   */
  CONTINUE_OFFLINE
}
