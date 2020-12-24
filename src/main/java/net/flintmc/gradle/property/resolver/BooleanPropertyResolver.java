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

/**
 * Resolver for boolean values.
 */
public class BooleanPropertyResolver extends FlintPluginPropertyResolver<Boolean> {
  @Override
  public Boolean resolve(FlintPluginProperty<Boolean> property, Project project) {
    String value = resolveRaw(property, project);
    if(value == null) {
      return null;
    }

    switch(value.toLowerCase()) {
      case "yes":
      case "y":
      case "1":
      case "true":
        return true;

      case "no":
      case "n":
      case "0":
      case "false":
        return false;

      default:
        throw new IllegalArgumentException("Failed to parse " + value + " as a boolean, use true or false");
    }
  }
}
