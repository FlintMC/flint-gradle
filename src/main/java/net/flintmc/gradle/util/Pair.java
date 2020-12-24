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
 * Generic pair class
 *
 * @param <A> The type of the first element
 * @param <B> The type of the second element
 */
public class Pair<A, B> {
  private final A first;
  private final B second;

  /**
   * Constructs a new {@link Pair} with the given values.
   *
   * @param first  The first value
   * @param second The second value
   */
  public Pair(A first, B second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Retrieves the first value.
   *
   * @return The first value
   */
  public A getFirst() {
    return first;
  }

  /**
   * Retrieves the second value.
   *
   * @return The second value
   */
  public B getSecond() {
    return second;
  }
}
