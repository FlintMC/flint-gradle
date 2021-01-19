package net.flintmc.gradle.patch.context;

import java.io.IOException;
import java.util.List;
import net.flintmc.gradle.patch.PatchSingle;

/** Represents a context provider for patch files. */
public interface PatchContextProvider {

  /**
   * Retrieves a collection with all lines of the given {@code patch} file.
   *
   * @param patch A single patch file.
   * @return A collection with all lines of the given {@code patch} file.
   * @throws IOException If an I/O error has occurred.
   */
  List<String> getData(PatchSingle patch) throws IOException;

  /**
   * Changes the lines of the given {@code patch}.
   *
   * @param patch A single patch file.
   * @param data The new collection of lines for the patch file.
   * @throws IOException If an I/O error has occurred.
   */
  void setData(PatchSingle patch, List<String> data) throws IOException;

  /**
   * Sets the given patch file as failed.
   *
   * @param patch A single patch file.
   * @param lines The new collection of lines for the patch file.
   * @throws IOException If an I/O error has occurred.
   */
  void setFailed(PatchSingle patch, List<String> lines) throws IOException;
}
