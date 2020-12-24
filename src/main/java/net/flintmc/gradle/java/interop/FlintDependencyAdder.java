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

package net.flintmc.gradle.java.interop;

import net.flintmc.gradle.extension.FlintGradleExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Utility class for adding dependencies using a versioned name
 */
public class FlintDependencyAdder implements BiConsumer<String, Action<DependencyHandler>> {
  private final Project project;
  private final Map<String, DependencyHandler> proxiedDependencyHandlers;
  private final FlintGradleExtension extension;

  /**
   * Constructs a new {@link FlintDependencyAdder} and initializes it with an empty map of proxied handlers.
   *
   * @param project The project to create the dependency adder for
   */
  public FlintDependencyAdder(Project project) {
    this.project = project;
    this.proxiedDependencyHandlers = new HashMap<>();
    this.extension = project.getExtensions().getByType(FlintGradleExtension.class);
  }

  @Override
  public void accept(String version, Action<DependencyHandler> dependencyHandlerAction) {
    // The extension needs be configured by now
    extension.ensureConfigured();

    if(!extension.getMinecraftVersions().contains(version)) {
      throw new IllegalArgumentException("Can't define dependencies for a minecraft version not part of the project");
    }

    // Retrieve or create a dependency handler proxy
    DependencyHandler handler = proxiedDependencyHandlers.computeIfAbsent(version, this::createProxy);
    dependencyHandlerAction.execute(handler);
  }

  /**
   * Creates a new dependency handler proxy for the given minecraft version.
   *
   * @param version The minecraft version to create the proxy for
   * @return The created proxy
   */
  private DependencyHandler createProxy(String version) {
    return DependencyHandlerProxy.of(project.getDependencies(), project.getConfigurations(), version);
  }

  /**
   * Retrieves the flint version this project is using.
   *
   * @return Retrieves the flint version this project is using
   */
  public String getFlintVersion() {
    extension.ensureConfigured();
    return extension.getFlintVersion();
  }
}
