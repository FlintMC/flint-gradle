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

package net.flintmc.gradle.extension;

import groovy.lang.Closure;
import net.flintmc.gradle.util.Pair;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import javax.annotation.Nonnull;
import java.util.*;

public class FlintRunsExtension implements Configurable<FlintRunsExtension> {
  private final Set<String> allIncludedConfigurations;
  private final Map<String, Set<String>> includedSourceSets;
  private final Map<String, Set<String>> excludedSourceSets;
  private final Map<String, String> mainClassOverrides;
  private final List<Pair<Object, Object>> arguments;
  private final List<Pair<Object, Object>> jvmArguments;
  private String generalMainClassOverride;

  /**
   * Constructs a {@link FlintRunsExtension} with default values.
   */
  public FlintRunsExtension() {
    this.allIncludedConfigurations = new HashSet<>();
    this.allIncludedConfigurations.add("main");
    this.includedSourceSets = new HashMap<>();
    this.excludedSourceSets = new HashMap<>();
    this.mainClassOverrides = new HashMap<>();
    this.arguments = new ArrayList<>();
    this.jvmArguments = new ArrayList<>();

    this.generalMainClassOverride = null;
  }

  /**
   * Constructs a {@link FlintRunsExtension} copying from the given parent extension.
   *
   * @param parent The parent to copy values from
   */
  public FlintRunsExtension(FlintRunsExtension parent) {
    this.allIncludedConfigurations = new HashSet<>(parent.allIncludedConfigurations);
    this.includedSourceSets = new HashMap<>(parent.includedSourceSets);
    this.excludedSourceSets = new HashMap<>(parent.excludedSourceSets);

    this.arguments = new ArrayList<>(parent.arguments);
    this.jvmArguments = new ArrayList<>(parent.jvmArguments);

    // The following can only be configured in the root project
    this.mainClassOverrides = null;
    this.generalMainClassOverride = null;
  }

  /**
   * Adds a configuration all available source sets should be included in.
   *
   * @param configuration The name of the run configuration to be included in
   */
  public void include(String configuration) {
    this.allIncludedConfigurations.add(configuration);
  }

  /**
   * Adds a list of source sets or versions to include in a specific configuration.
   *
   * @param configuration       The name of the run configuration to be included in
   * @param versionOrSourceSets The source sets or versions to include
   */
  public void include(String configuration, String... versionOrSourceSets) {
    List<String> versionList = Arrays.asList(versionOrSourceSets);

    // Add the configurations to the list
    this.includedSourceSets.computeIfAbsent(configuration, (k) -> new HashSet<>()).addAll(versionList);

    // Probe for exclusion from the list
    Set<String> excludedConfigurations = this.excludedSourceSets.get(configuration);
    if (excludedConfigurations != null) {
      // Remove exclusions
      excludedConfigurations.removeAll(versionList);
    }
  }

  /**
   * Excludes this project from the given configuration.
   *
   * @param configuration The name of the run configuration to remove this project from
   */
  public void exclude(String configuration) {
    this.allIncludedConfigurations.remove(configuration);
  }

  /**
   * Excludes a list of source sets or versions from the given configuration.
   *
   * @param configuration        Tne name of the run configuration to exclude the source sets or versions from
   * @param sourceSetsOrVersions The source sets or versions to exclude
   */
  public void exclude(String configuration, String... sourceSetsOrVersions) {
    List<String> versionList = Arrays.asList(sourceSetsOrVersions);

    // Add the configuration to the list
    this.excludedSourceSets.computeIfAbsent(configuration, (k) -> new HashSet<>()).addAll(versionList);

    // Probe for inclusions of the list
    Set<String> versionedConfigurations = this.includedSourceSets.get(configuration);
    if (versionedConfigurations != null) {
      // Remove inclusions
      versionedConfigurations.removeAll(versionList);
    }
  }

  /**
   * Retrieves a list of configurations which include all source sets.
   *
   * @return A list of configurations including all source sets and the matching version
   */
  public Set<String> getAllIncludedConfigurations() {
    return allIncludedConfigurations;
  }

  /**
   * Retrieves map of source sets or versions excluded from specific configurations.
   *
   * @return A map of source sets or versions excluded from specific configurations
   */
  public Map<String, Set<String>> getExcludedSourceSets() {
    return excludedSourceSets;
  }

  /**
   * Retrieves a map of source sets or versions included in specific configurations.
   *
   * @return A map of source sets or versions included in specific configurations
   */
  public Map<String, Set<String>> getIncludedSourceSets() {
    return includedSourceSets;
  }

  /**
   * Retrieves a collection of arguments.
   *
   * @return A collection of arguments.
   */
  public List<Pair<Object, Object>> getArguments() {
    return arguments;
  }

  /**
   * Retrieves a collection of jvm arguments.
   *
   * @return A collection of jvm arguments.
   */
  public List<Pair<Object, Object>> getJvmArguments() {
    return jvmArguments;
  }

  /**
   * Overrides the main class of the given configuration.
   *
   * @param configuration The configuration to override the main class of
   * @param newMainClass  The new main class of the configuration
   * @throws IllegalStateException If this runs extension is not the one of the root project
   */
  public void overrideMainClass(String configuration, String newMainClass) {
    if (mainClassOverrides == null) {
      throw new IllegalStateException("Overriding the main classes can only be done in the root project");
    }

    mainClassOverrides.put(configuration, newMainClass);
  }

  /**
   * Retrieves a map of all configurations mapped to their main class overrides.
   *
   * @return A map of all main class overrides
   * @throws IllegalStateException If this runs extension is not the one of the root project
   */
  public Map<String, String> getMainClassOverrides() {
    if (mainClassOverrides == null) {
      throw new IllegalStateException("The main class overrides can only be retrieved from the root project");
    }

    return mainClassOverrides;
  }

  /**
   * Overrides the main class for all configurations. Overrides set with {@link #overrideMainClass(String, String)}
   * have precedence.
   *
   * @param newMainClass The new main class for all configurations, or {@code null} to set the default
   * @throws IllegalStateException If this runs extension is not the one of the root project
   */
  public void overrideMainClass(String newMainClass) {
    if (mainClassOverrides == null) {
      throw new IllegalStateException("Overriding the main classes can only be done in the root project");
    }

    generalMainClassOverride = newMainClass;
  }

  /**
   * Retrieves the main class override which should apply to all configurations which
   * don't have their main class overridden explicitly.
   *
   * @return The general main class override, or {@code null} if none
   * @throws IllegalStateException If this runs extension is not the one of the root project
   */
  public String getGeneralMainClassOverride() {
    if (mainClassOverrides == null) {
      throw new IllegalStateException("The main class override can only be retrieved from the root project");
    }

    return generalMainClassOverride;
  }

  /**
   * Adds an argument to use launch the JVM for the process.
   */
  public void jvmArg(Object key, Object value) {
    this.jvmArguments.add(new Pair<>(key, value));
  }

  /**
   * Adds argument for the main class to be executed.
   *
   */
  public void arg(Object key, Object value) {
    this.arguments.add(new Pair<>(key, value));
  }

  /**
   * Configures this runs extension with the given closure.
   *
   * @param closure The closure to use for configuration
   * @return this
   */
  @Override
  @Nonnull
  public FlintRunsExtension configure(@Nonnull Closure closure) {
    return ConfigureUtil.configureSelf(closure, this);
  }
}
