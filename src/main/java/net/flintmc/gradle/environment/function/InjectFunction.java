package net.flintmc.gradle.environment.function;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import net.flintmc.gradle.environment.DeobfuscationException;
import net.flintmc.gradle.environment.DeobfuscationUtilities;
import net.flintmc.gradle.util.Util;

public class InjectFunction extends Function {

  private final Path input;
  private final String environmentName;

  /**
   * Constructs a new function with the given {@code name} and {@code output}.
   *
   * @param name The name of the function.
   * @param output The output of the function.
   * @param input The input of the function.
   * @param environmentName The environment name of the function.
   */
  public InjectFunction(String name, Path output, Path input, String environmentName) {
    super(name, output);
    this.input = input;
    this.environmentName = environmentName;
  }

  /** {@inheritDoc} */
  @Override
  public void execute(DeobfuscationUtilities utilities) throws DeobfuscationException {

    try (ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(this.input));
        ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(this.output))) {

      ZipEntry entry;

      while ((entry = inputStream.getNextEntry()) != null) {
        outputStream.putNextEntry(entry);

        Util.copyStream(inputStream, outputStream);

        outputStream.closeEntry();
      }

      entry = new ZipEntry("." + this.environmentName + "-processed");
      outputStream.putNextEntry(entry);
      outputStream.closeEntry();
    } catch (IOException exception) {
      throw new DeobfuscationException(
          "Failed to execute inject function named " + name, exception);
    }
  }
}
