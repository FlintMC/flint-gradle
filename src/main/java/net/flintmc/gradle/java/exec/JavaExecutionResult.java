/*
 * FlintMC
 * Copyright (C) 2020-2021 LabyMedia GmbH and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.flintmc.gradle.java.exec;

/**
 * Represents the result of a java execution.
 */
public class JavaExecutionResult {
  private final int exitCode;
  private final String stdout;
  private final String stderr;

  /**
   * Constructs a new {@link JavaExecutionResult} with the given exit code, process standard output and process
   * standard error.
   *
   * @param exitCode The exit code of the process
   * @param stdout   The standard output of the process
   * @param stderr   The standard error of the process
   */
  public JavaExecutionResult(int exitCode, String stdout, String stderr) {
    this.exitCode = exitCode;
    this.stdout = stdout;
    this.stderr = stderr;
  }

  /**
   * Retrieves the exit code of the process.
   *
   * @return The exit code of the process
   */
  public int getExitCode() {
    return exitCode;
  }

  /**
   * Retrieves the standard output of the process.
   *
   * @return The standard output of the process
   */
  public String getStdout() {
    return stdout;
  }

  /**
   * Retrieves the standard error of the process.
   *
   * @return The standard error of the process
   */
  public String getStderr() {
    return stderr;
  }
}
