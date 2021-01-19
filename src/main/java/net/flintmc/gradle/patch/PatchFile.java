package net.flintmc.gradle.patch;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.flintmc.gradle.patch.function.PatchSupplier;

public final class PatchFile {

  private final PatchSupplier supplier;
  private final boolean requiresFurtherProcessing;

  /**
   * Constructs a new {@link PatchFile} with the given parameters.
   *
   * @param supplier The supplier for the patch.
   * @param requiresFurtherProcessing {@code true} if further processing is required, otherwise
   *     {@code false}.
   */
  private PatchFile(PatchSupplier supplier, boolean requiresFurtherProcessing) {
    this.supplier = supplier;
    this.requiresFurtherProcessing = requiresFurtherProcessing;
  }

  /**
   * Retrieves a new {@link PatchFile}
   *
   * @param content The content to open.
   * @return A new patch file.
   */
  public static PatchFile from(String content) {
    return from(content.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Retrieves a new {@link PatchFile} from the given {@code data} byte array.
   *
   * @param data The byte array to open.
   * @return A new patch file.
   */
  public static PatchFile from(byte[] data) {
    return new PatchFile(() -> new ByteArrayInputStream(data), false);
  }

  /**
   * Retrieves a new {@link PatchFile} from the given {@link File}.
   *
   * @param file The file to open.
   * @return A new patch file.
   */
  public static PatchFile from(File file) {
    return new PatchFile(() -> new FileInputStream(file), true);
  }

  /**
   * Opens the {@link InputStream} of this patch file.
   *
   * @return The input stream of this patch file.
   * @throws IOException If an I/O error has occurred.
   */
  public InputStream openStream() throws IOException {
    return this.supplier.get();
  }

  /**
   * Whether if further processing is required.
   *
   * @return {@code true} if further processing is required, otherwise {@code false}.
   */
  public boolean requiresFurtherProcessing() {
    return this.requiresFurtherProcessing;
  }
}
