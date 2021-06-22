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

package net.flintmc.gradle.java.instrumentation.api;

/**
 * Entry point of an instrumentation.
 * Requires a Java Service to be registered and findable via a {@link java.util.ServiceLoader}.
 * This class exists only to make it possible to use things like dependency injection in a {@link net.flintmc.gradle.java.instrumentation.api.transformer.InstrumentationTransformer}.
 * If the {@link java.util.ServiceLoader} would look for {@link net.flintmc.gradle.java.instrumentation.api.transformer.InstrumentationTransformer} this would not be possible to do.
 * Furthermore, it is possible to specify more details about the {@link net.flintmc.gradle.java.instrumentation.api.transformer.InstrumentationTransformer} to register.
 *
 * @see InstrumentationTransformerRegistry
 */
public interface InstrumentationTransformerRegistrator {

  /**
   * Initialize the instrumentation chain.
   *
   * @param registry the transformer registry to register all transformers on and retrieve information about the current transformation
   */
  void initialize(InstrumentationTransformerRegistry registry);

}
