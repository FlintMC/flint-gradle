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

package net.flintmc.gradle.extension;

import groovy.lang.Closure;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.util.Configurable;

import javax.annotation.Nonnull;

/**
 * Static files block in the flint extension.
 */
public class FlintStaticFilesExtension implements Configurable<NamedDomainObjectContainer<FlintStaticFileDescription>> {
  private final NamedDomainObjectContainer<FlintStaticFileDescription> staticFileDescriptions;

  /**
   * Constructs a new {@link FlintStaticFilesExtension} for the given project.
   *
   * @param project The project owning this extension
   */
  public FlintStaticFilesExtension(Project project) {
    this.staticFileDescriptions = project.container(
        FlintStaticFileDescription.class, (name) -> new FlintStaticFileDescription(project, name));
  }

  /**
   * Retrieves the container of static file descriptions.
   *
   * @return The container of static file descriptions
   */
  public NamedDomainObjectContainer<FlintStaticFileDescription> getStaticFileDescriptions() {
    return staticFileDescriptions;
  }

  @Override
  @Nonnull
  public NamedDomainObjectContainer<FlintStaticFileDescription> configure(@Nonnull Closure cl) {
    return staticFileDescriptions.configure(cl);
  }
}
