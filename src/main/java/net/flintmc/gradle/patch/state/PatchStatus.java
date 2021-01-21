package net.flintmc.gradle.patch.state;

/** Represents an enumeration with all available patch statuses. */
public enum PatchStatus {
  /** When the patch successfully patches the file. */
  PATCHED(true),
  /** When the patch is missing. */
  MISSING(false),
  /** When the patch has failed. */
  FAILURE(false),
  /** When the patch is skipped. */
  SKIPPED(true),
  /** When the patch was attempted. */
  TRIED(true);

  private final boolean success;

  PatchStatus(boolean success) {
    this.success = success;
  }

  /**
   * Whether the patch status was successful.
   *
   * @return {@code true} if the patch status was successful, otherwise {@code false}.
   */
  public boolean isSuccess() {
    return this.success;
  }
}
