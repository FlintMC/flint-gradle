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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Objects;

public class VersionedArguments {
  @JsonDeserialize(using = ArgumentString.ListDeserializer.class)
  private List<ArgumentString> game;

  @JsonDeserialize(using = ArgumentString.ListDeserializer.class)
  private List<ArgumentString> jvm;

  public List<ArgumentString> getGame() {
    return game;
  }

  public List<ArgumentString> getJvm() {
    return jvm;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VersionedArguments that = (VersionedArguments) o;
    return Objects.equals(game, that.game) &&
        Objects.equals(jvm, that.jvm);
  }

  @Override
  public int hashCode() {
    return Objects.hash(game, jvm);
  }
}
