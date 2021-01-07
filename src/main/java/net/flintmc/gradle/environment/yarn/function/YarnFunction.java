package net.flintmc.gradle.environment.yarn.function;

import java.nio.file.Path;
import net.flintmc.gradle.environment.function.Function;

public abstract class YarnFunction extends Function {

  /**
   * Constructs a new function with the given {@code name} and {@code output}.
   *
   * @param name The name of the function.
   * @param output The output of the function.
   */
  protected YarnFunction(String name, Path output) {
    super(name, output);
  }
}
