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

package net.flintmc.gradle.java.instrumentation;

import java.util.HashSet;
import java.util.Set;
import net.flintmc.gradle.java.instrumentation.api.InstrumentationTransformer;

public final class InstrumentationTransformerRegistry {

  private final Set<InstrumentationTransformer> transformers;

  public InstrumentationTransformerRegistry() {
    this.transformers = new HashSet<>();
  }

  /**
   * Registers a new {@link InstrumentationTransformer}.
   *
   * @param transformer The instrumentation transformer to be registered.
   */
  public void register(final InstrumentationTransformer transformer) {
    this.transformers.add(transformer);
  }

  /**
   * Retrieves a collection with all registered {@link InstrumentationTransformer}`s.
   *
   * @return A collection with all registered instrumentation transformers.
   */
  public Set<InstrumentationTransformer> getTransformers() {
    return this.transformers;
  }

  /**
   * Retrieves the number of elements of the {@link InstrumentationTransformerRegistry#transformers}
   * collection.
   *
   * @return The number of elements of the {@link InstrumentationTransformerRegistry#transformers}
   * collection.
   */
  public int size() {
    return this.transformers.size();
  }

}
