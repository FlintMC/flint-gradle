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

import java.nio.file.Path;
import net.flintmc.gradle.environment.function.Function;

/** Base class for all MCP functions. */
public abstract class MCPFunction extends Function {

  /**
   * Constructs a new function with the given {@code name} and {@code output}.
   *
   * @param name The name of the function.
   * @param output The output of the function.
   */
  protected MCPFunction(String name, Path output) {
    super(name, output);
  }
}
