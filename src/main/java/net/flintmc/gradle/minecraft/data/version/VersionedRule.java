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

package net.flintmc.gradle.minecraft.data.version;

import java.util.Map;
import java.util.Objects;

public class VersionedRule {
  private VersionedRuleAction action;
  private Map<String, Object> features;
  private VersionedOsRule os;

  public VersionedRuleAction getAction() {
    return action;
  }

  public Map<String, Object> getFeatures() {
    return features;
  }

  public VersionedOsRule getOs() {
    return os;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VersionedRule that = (VersionedRule) o;
    return action == that.action &&
        Objects.equals(features, that.features) &&
        Objects.equals(os, that.os);
  }

  @Override
  public int hashCode() {
    return Objects.hash(action, features, os);
  }
}
