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

package net.flintmc.gradle.extension.json;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.util.Configurable;

import javax.annotation.Nonnull;

/**
 * Json injections block in the flint extension.
 */
public class FlintJsonInjectionsExtension implements Configurable<NamedDomainObjectContainer<FlintJsonInjectionDescription>> {
  private final NamedDomainObjectContainer<FlintJsonInjectionDescription> jsonInjectionDescriptions;

  /**
   * Constructs a new {@link FlintJsonInjectionsExtension} for the given project.
   *
   * @param project The project owning this extension
   */
  public FlintJsonInjectionsExtension(Project project) {
    this.jsonInjectionDescriptions = project.container(
        FlintJsonInjectionDescription.class, (name) -> new FlintJsonInjectionDescription(project, name));
  }

  /**
   * Retrieves the container of json injection descriptions.
   *
   * @return The container of json injection descriptions
   */
  public NamedDomainObjectContainer<FlintJsonInjectionDescription> getJsonInjectionDescriptions() {
    return jsonInjectionDescriptions;
  }

  @Override
  @Nonnull
  public NamedDomainObjectContainer<FlintJsonInjectionDescription> configure(@Nonnull Closure cl) {
    return jsonInjectionDescriptions.configure(cl);
  }
}
