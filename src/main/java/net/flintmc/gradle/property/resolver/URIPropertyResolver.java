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

package net.flintmc.gradle.property.resolver;

import net.flintmc.gradle.property.FlintPluginProperty;
import org.gradle.api.Project;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Resolver for URI/URL properties.
 */
public class URIPropertyResolver extends FlintPluginPropertyResolver<URI> {
  @Override
  public URI resolve(FlintPluginProperty<URI> property, Project project) {
    String value = resolveRaw(property, project);
    if(value == null) {
      return null;
    }

    try {
      return new URI(value);
    } catch(URISyntaxException e) {
      throw new IllegalArgumentException("Failed to parse URI", e);
    }
  }
}
