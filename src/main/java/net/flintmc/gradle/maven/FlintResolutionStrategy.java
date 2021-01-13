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

package net.flintmc.gradle.maven;

import java.util.ArrayList;
import java.util.List;
import org.gradle.api.Project;

public class FlintResolutionStrategy {

  private final List<String> forcedDependencies;

  public FlintResolutionStrategy() {
    this.forcedDependencies = new ArrayList<>();

    this.forcedDependencies.add("org.apache.logging.log4j:log4j-api:2.8.2");
    this.forcedDependencies.add("com.google.guava:guava:27.0.1-jre");
    this.forcedDependencies.add("org.apache.commons:commons-lang3:3.10");
    this.forcedDependencies.add("org.apache.logging.log4j:log4j-core:2.8.2");
    this.forcedDependencies.add("it.unimi.dsi:fastutil:8.2.1");
    this.forcedDependencies.add("net.java.dev.jna:jna:4.4.0");
    this.forcedDependencies.add("com.google.code.findbugs:jsr305:3.0.2");
    this.forcedDependencies.add("com.google.code.gson:gson:2.8.6");
    this.forcedDependencies.add("commons-io:commons-io:2.6");
    this.forcedDependencies.add("commons-codec:commons-codec:1.10");
    this.forcedDependencies.add("com.beust:jcommander:1.78");
  }

  /** Forces a list of dependencies. */
  public void forceDependencies(Project project) {
    project
        .getConfigurations()
        .forEach(
            configuration ->
                configuration.resolutionStrategy(
                    resolutionStrategy -> forcedDependencies.forEach(resolutionStrategy::force)));
  }
}
