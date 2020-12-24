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

package net.flintmc.gradle.util;

/**
 * Wrapper for possible {@code null} values. This is required because Gradle does not allow passing {@code null} to
 * injected constructors, which is required in some cases in this plugin.
 *
 * @param <T> The contained type
 */
public class MaybeNull<T> {
  protected T value;

  /**
   * Constructs a new {@link MaybeNull}.
   *
   * @param value The contained value
   */
  public MaybeNull(T value) {
    this.value = value;
  }

  /**
   * Retrieves the contained value
   *
   * @return The contained value, or {@code null}
   */
  public T get() {
    return value;
  }
}
