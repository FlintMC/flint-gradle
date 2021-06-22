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

package net.flintmc.gradle.maven;

import net.flintmc.gradle.extension.FlintGradleExtension;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionSelector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FlintResolutionStrategy {

  private static final FlintResolutionStrategy instance = new FlintResolutionStrategy();
  private final List<Object> forcedDependencies;

  private FlintResolutionStrategy() {
    this.forcedDependencies = new ArrayList<>();

    // TODO: 21.01.2021 This should perhaps be handled via an endpoint so that we don't always have
    // to update the plugin. But for the moment it is enough like this.
    this.forceDependency("org.apache.logging.log4j:log4j-api:2.8.2");
    this.forceDependency("com.google.guava:guava:27.0.1-jre");
    this.forceDependency("org.apache.commons:commons-lang3:3.10");
    this.forceDependency("org.apache.logging.log4j:log4j-core:2.8.2");
    this.forceDependency("it.unimi.dsi:fastutil:8.2.1");
    this.forceDependency("net.java.dev.jna:jna:4.4.0");
    this.forceDependency("com.google.code.findbugs:jsr305:3.0.2");
    this.forceDependency("com.google.code.gson:gson:2.8.6");
    this.forceDependency("commons-io:commons-io:2.6");
    this.forceDependency("commons-codec:commons-codec:1.10");
    this.forceDependency("com.beust:jcommander:1.78");
    this.forceDependency("com.google.inject:guice:4.2.3");
  }

  public static FlintResolutionStrategy getInstance() {
    return instance;
  }

  /**
   * Allows forcing certain versions of dependencies, including transitive dependencies. Appends new forced modules to
   * be considered when resolving dependencies. It accepts following notations:
   *
   * <ul>
   *   <li>String in a format of: 'group:name:version', for example: 'org.gradle:gradle-core:1.0'
   *   <li>Instance of {@link ModuleVersionSelector}
   * </ul>
   *
   * @param moduleVersionSelectorNotation Typically group:name:version notation to append.
   */
  public void forceDependency(Object moduleVersionSelectorNotation) {
    ForcedDependency forcedDependency = this.createForcedDependency(moduleVersionSelectorNotation);

    if(forcedDependency == null) {
      return;
    }

    for(int index = 0; index < this.forcedDependencies.size(); index++) {
      ForcedDependency dependency = this.createForcedDependency(this.forcedDependencies.get(index));

      if(dependency == null) {
        return;
      }

      if(dependency.equals(forcedDependency)) {
        return;
      }

      if(dependency.nonVersionEquals(forcedDependency)) {
        this.forcedDependencies.set(index, moduleVersionSelectorNotation);
        return;
      }
    }

    this.forcedDependencies.add(moduleVersionSelectorNotation);
  }

  /**
   * Allows forcing certain versions of dependencies, including transitive dependencies. Appends new forced modules to
   * be considered when resolving dependencies. It accepts following notations:
   *
   * <ul>
   *   <li>String in a format of: 'group:name:version', for example: 'org.gradle:gradle-core:1.0'
   *   <li>Instance of {@link ModuleVersionSelector}
   *   <li>Any collection or array of above will be automatically flattened
   * </ul>
   *
   * @param moduleVersionSelectorNotations Typically group:name:version notations to append.
   */
  public void forceDependencies(Object... moduleVersionSelectorNotations) {
    for(Object moduleVersionSelectorNotation : moduleVersionSelectorNotations) {
      this.forceDependency(moduleVersionSelectorNotation);
    }
  }

  /**
   * Forces a collection of dependencies on all configurations of the given {@code project}.
   *
   * @param project   The project for which the dependencies are to be enforced.
   * @param extension The flint extension for the project
   */
  public void forceResolutionStrategy(Project project, FlintGradleExtension extension) {
    String flintVersion = extension.getFlintVersion();

    project
        .getConfigurations()
        .forEach(
            configuration ->
                configuration.resolutionStrategy(
                    resolutionStrategy -> {
                      forcedDependencies.forEach(resolutionStrategy::force);
                      resolutionStrategy.eachDependency((details) -> {
                        ModuleVersionSelector selector = details.getRequested();

                        if(selector.getGroup().equals("net.flintmc")
                            && !Objects.equals(selector.getVersion(), flintVersion)
                            && !selector.getName().equals("flint-gradle")) {

                          // Force the flint version to what is specified in the project
                          details.useVersion(flintVersion);
                          details.because(
                              "Forcing net.flintmc dependency from " + selector.getVersion() + " to " +
                                  flintVersion + " to ensure a consistent framework version " +
                                  "[Automatically done by flint-gradle]");
                        }
                      });
                    }));
  }

  private ForcedDependency createForcedDependency(Object moduleVersionSelectorNotation) {

    if(moduleVersionSelectorNotation instanceof String) {
      String notation = (String) moduleVersionSelectorNotation;

      if(!notation.contains(":")) {
        return null;
      }

      String[] split = notation.split(":");

      if(split.length == 3) {

        String group = split[0];
        String name = split[1];
        String version = split[2];

        return new ForcedDependency(group, name, version);
      }
    } else if(moduleVersionSelectorNotation instanceof ModuleVersionSelector) {
      ModuleVersionSelector moduleVersionSelector =
          (ModuleVersionSelector) moduleVersionSelectorNotation;
      return new ForcedDependency(
          moduleVersionSelector.getGroup(),
          moduleVersionSelector.getName(),
          moduleVersionSelector.getVersion());
    }
    return null;
  }

  private static class ForcedDependency {

    private final String group;
    private final String name;
    private final String version;

    public ForcedDependency(String group, String name, String version) {
      this.group = group;
      this.name = name;
      this.version = version;
    }

    public String getGroup() {
      return group;
    }

    public String getName() {
      return name;
    }

    public String getVersion() {
      return version;
    }

    public boolean nonVersionEquals(Object o) {
      if(this == o) {
        return true;
      }
      if(o == null || getClass() != o.getClass()) {
        return false;
      }
      ForcedDependency that = (ForcedDependency) o;
      return Objects.equals(group, that.group) && Objects.equals(name, that.name);
    }

    @Override
    public boolean equals(Object o) {
      if(this == o) {
        return true;
      }
      if(o == null || getClass() != o.getClass()) {
        return false;
      }
      ForcedDependency that = (ForcedDependency) o;
      return Objects.equals(group, that.group)
          && Objects.equals(name, that.name)
          && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
      return Objects.hash(group, name, version);
    }
  }
}
