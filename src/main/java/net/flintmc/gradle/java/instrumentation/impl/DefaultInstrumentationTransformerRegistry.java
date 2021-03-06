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

package net.flintmc.gradle.java.instrumentation.impl;

import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.java.instrumentation.api.InstrumentationTransformerRegistry;
import net.flintmc.gradle.java.instrumentation.api.context.InstrumentationTransformerContext;
import net.flintmc.gradle.java.instrumentation.api.transformer.InstrumentationTransformer;
import org.gradle.api.tasks.SourceSet;

import java.util.*;
import java.util.function.Predicate;

public class DefaultInstrumentationTransformerRegistry implements InstrumentationTransformerRegistry {

  private final Map<InstrumentationTransformer, Predicate<InstrumentationTransformerContext>> transformers = new HashMap<>();
  private final SourceSet sourceSet;
  private final FlintGradlePlugin flintGradlePlugin;

  public DefaultInstrumentationTransformerRegistry(SourceSet sourceSet, FlintGradlePlugin flintGradlePlugin) {
    this.sourceSet = sourceSet;
    this.flintGradlePlugin = flintGradlePlugin;
  }

  @Override
  public InstrumentationTransformerRegistry registerTransformer(InstrumentationTransformer instrumentationTransformer) {
    return this.registerTransformer(instrumentationTransformer, instrumentationTransformerContext -> true);
  }

  @Override
  public InstrumentationTransformerRegistry registerTransformer(InstrumentationTransformer instrumentationTransformer, Predicate<InstrumentationTransformerContext> transformerPredicate) {
    if (this.transformers.containsKey(instrumentationTransformer)) {
      throw new IllegalStateException("Class Transformer got registered multiple times");
    }
    this.transformers.put(instrumentationTransformer, transformerPredicate);
    return this;
  }

  @Override
  public Map<InstrumentationTransformer, Predicate<InstrumentationTransformerContext>> getTransformers() {
    return Collections.unmodifiableMap(transformers);
  }

  @Override
  public SourceSet getSourceSet() {
    return sourceSet;
  }

  @Override
  public FlintGradlePlugin getGradlePlugin() {
    return this.flintGradlePlugin;
  }

}
