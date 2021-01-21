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
