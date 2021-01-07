package net.flintmc.gradle.environment;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public interface EnvironmentRunnable {

  void loadData() throws FileNotFoundException, IOException;

  /**
   * Prepares all steps for of the given side for execution.
   *
   * @param side The side to prepare the steps for.
   * @throws DeobfuscationException If a step fails to prepare.
   */
  void prepare(String side) throws DeobfuscationException;

  /**
   * Runs all steps for the given side driving the run to completion.
   *
   * @param side The side to execute the steps of.
   * @return The output of the last step.
   * @throws DeobfuscationException If a step fails to run.
   */
  Path execute(String side) throws DeobfuscationException;

}
