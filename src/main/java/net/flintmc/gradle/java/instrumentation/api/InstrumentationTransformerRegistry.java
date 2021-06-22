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

import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.java.instrumentation.api.context.InstrumentationTransformerContext;
import net.flintmc.gradle.java.instrumentation.api.transformer.InstrumentationTransformer;
import org.gradle.api.tasks.SourceSet;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Util class to provide context between {@link InstrumentationTransformerRegistrator} and {@link InstrumentationTransformerContext}.
 */
public interface InstrumentationTransformerRegistry {

  /**
   * Register a given {@link InstrumentationTransformer} for all outputs of the current {@link SourceSet}.
   *
   * @param instrumentationTransformer the transformer to register
   * @return this
   */
  InstrumentationTransformerRegistry registerTransformer(InstrumentationTransformer instrumentationTransformer);

  /**
   * Register a given {@link InstrumentationTransformer} for all outputs of the current {@link SourceSet} matching a given {@link Predicate}.
   *
   * @param instrumentationTransformer the transformer to register
   * @param predicate                  the predicate to check if the current resource should be transformed
   * @return this
   */
  InstrumentationTransformerRegistry registerTransformer(InstrumentationTransformer instrumentationTransformer, Predicate<InstrumentationTransformerContext> predicate);

  /**
   * @return all registered transformers
   */
  Map<InstrumentationTransformer, Predicate<InstrumentationTransformerContext>> getTransformers();

  /**
   * @return the source set of the current instrumentation round
   */
  SourceSet getSourceSet();

  /**
   * @return the singleton instance of the flint gradle plugin
   */
  FlintGradlePlugin getGradlePlugin();

}
