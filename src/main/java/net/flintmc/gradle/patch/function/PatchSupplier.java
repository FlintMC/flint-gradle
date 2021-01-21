package net.flintmc.gradle.patch.function;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a supplier of an {@link InputStream}.
 *
 * <p>It is not required that a new or different input stream be returned each time the provider is
 * called.
 */
@FunctionalInterface
public interface PatchSupplier {

  /**
   * Retrieves an {@link InputStream}.
   *
   * @return An input stream.
   * @throws IOException If an I/O error has occurred.
   */
  InputStream get() throws IOException;
}
