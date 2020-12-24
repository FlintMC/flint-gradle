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

package net.flintmc.gradle.environment.mcp.function;

import net.flintmc.gradle.environment.DeobfuscationException;
import net.flintmc.gradle.environment.DeobfuscationUtilities;

import java.nio.file.Path;

/**
 * Base class for all MCP functions.
 */
public abstract class MCPFunction {
  protected final String name;
  protected final Path output;

  /**
   * Constructs a new MCP function with the given name and output.
   *
   * @param name   The name of the function
   * @param output The output of the function
   */
  protected MCPFunction(String name, Path output) {
    this.name = name;
    this.output = output;
  }

  /**
   * Retrieves the name of this MCP function.
   *
   * @return The name of this MCP function
   */
  public String getName() {
    return name;
  }

  /**
   * Retrieves the output of this MCP function.
   *
   * @return The output of this MCP function
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
  public void prepare(DeobfuscationUtilities utilities) throws DeobfuscationException {
  }

  /**
   * Executes this MCP function.
   *
   * @param utilities The utilities which can be used during execution
   * @throws DeobfuscationException If the execution if this MCP function fails
   */
  public abstract void execute(DeobfuscationUtilities utilities) throws DeobfuscationException;
}
