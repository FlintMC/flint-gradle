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

import net.flintmc.gradle.maven.pom.MavenArtifact;

import java.net.URI;
import java.util.List;

/**
 * Represents a java MCP execution template.
 */
public class JavaExecutionTemplate {
  private final URI executionArtifactRepo;
  private final MavenArtifact executionArtifact;
  private final List<String> args;
  private final List<String> jvmArgs;

  /**
   * Constructs a new {@link JavaExecutionTemplate} with the given repository, artifact, arguments and JVM arguments.
   *
   * @param executionArtifactRepo The repository the artifact can be found in
   * @param executionArtifact     The artifact used for execution
   * @param args                  The argument templates passed to the execution
   * @param jvmArgs               The argument passed to the JVM executing the artifact
   */
  public JavaExecutionTemplate(
      URI executionArtifactRepo,
      MavenArtifact executionArtifact,
      List<String> args,
      List<String> jvmArgs
  ) {
    this.executionArtifactRepo = executionArtifactRepo;
    this.executionArtifact = executionArtifact;
    this.args = args;
    this.jvmArgs = jvmArgs;
  }

  /**
   * Retrieves the base URL of the repository the artifact of this execution can be found in.
   *
   * @return The base URL of the repository of this execution
   */
  public URI getExecutionArtifactRepo() {
    return executionArtifactRepo;
  }

  /**
   * Retrieves the artifact used for this execution.
   *
   * @return The artifact for this execution
   */
  public MavenArtifact getExecutionArtifact() {
    return executionArtifact;
  }

  /**
   * Retrieves the argument templates passed to the execution.
   *
   * @return The argument templates passed to this execution
   */
  public List<String> getArgs() {
    return args;
  }

  /**
   * Retrieves the JVM arguments passed to the JVM running the execution.
   *
   * @return The JVM arguments of this execution
   */
  public List<String> getJvmArgs() {
    return jvmArgs;
  }
}
