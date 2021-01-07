package net.flintmc.gradle.environment.function;

import java.nio.file.Path;
import net.flintmc.gradle.environment.DeobfuscationException;
import net.flintmc.gradle.environment.DeobfuscationUtilities;

/** Base class for all functions. */
public abstract class Function {

  protected final String name;
  protected final Path output;

  /**
   * Constructs a new function with the given {@code name} and {@code output}.
   *
   * @param name The name of the function.
   * @param output The output of the function.
   */
  protected Function(String name, Path output) {
    this.name = name;
    this.output = output;
  }

  /**
   * Retrieves the name of this function.
   *
   * @return The name of this function.
   */
  public String getName() {
    return name;
  }

  /**
   * Retrieves the output of this function.
   *
   * @return The output of this function.
   */
  public Path getOutput() {
    return output;
  }

  /**
   * Prepares the given function for execution.
   *
   * @param utilities The utilities which can be used during preparation
   * @throws DeobfuscationException If the preparations fail
   */
  public void prepare(DeobfuscationUtilities utilities) throws DeobfuscationException {}

  /**
   * Executes this function.
   *
   * @param utilities The utilities which can be used during execution
   * @throws DeobfuscationException If the execution if this MCP function fails
   */
  public abstract void execute(DeobfuscationUtilities utilities) throws DeobfuscationException;

}
