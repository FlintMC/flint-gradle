package net.flintmc.gradle.patch.state;

/** Represents an enumeration with all available patch modes. */
public enum PatchMode {
  /** Adding a new file. */
  ADD,
  /** Update to existing file */
  CHANGE,
  /** Deleting an existing file. */
  DELETE
}
