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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.flintmc.gradle.FlintGradlePlugin;
import net.flintmc.gradle.minecraft.data.environment.EnvironmentType;
import net.flintmc.gradle.minecraft.data.environment.MinecraftVersion;
import net.flintmc.gradle.util.Util;
import org.gradle.api.Action;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

/** Gradle extension block for configuring the plugin */
public class FlintGradleExtension implements Configurable<FlintGradleExtension> {
  public static final String NAME = "flint";

  private final FlintGradlePlugin plugin;
  private final FlintRunsExtension runsExtension;
  private final FlintStaticFilesExtension staticFilesExtension;
  private final FlintSelfInstallerExtension selfInstallerExtension;

  private boolean configured;

  private String[] authors;
  private String publishToken;
  private Set<MinecraftVersion> minecraftVersions;
  private Predicate<Project> projectFilter;
  private Type type = Type.PACKAGE;
  private String flintVersion;
  private boolean enablePublishing;
  private boolean autoConfigurePublishing;

  /**
   * Creates a new {@link FlintGradleExtension} with default values.
   *
   * @param plugin The plugin this extension belongs to
   */
  public FlintGradleExtension(FlintGradlePlugin plugin) {
    this.plugin = plugin;

    this.minecraftVersions = new HashSet<>();
    this.projectFilter = p -> p.getPluginManager().hasPlugin("java");
    this.runsExtension = new FlintRunsExtension();
    this.staticFilesExtension = new FlintStaticFilesExtension(plugin.getProject());
    this.selfInstallerExtension = new FlintSelfInstallerExtension();

    this.enablePublishing = true;
    this.autoConfigurePublishing = true;
    this.minecraftVersions = new HashSet<>();
  }

  /**
   * Creates a new {@link FlintGradleExtension} with values copied from a parent extension.
   *
   * @param plugin The plugin owning this extension
   * @param parent The parent extension
   */
  public FlintGradleExtension(FlintGradlePlugin plugin, FlintGradleExtension parent) {
    this.plugin = plugin;
    this.minecraftVersions = new HashSet<>(parent.minecraftVersions);
    this.projectFilter = parent.projectFilter;
    this.runsExtension = new FlintRunsExtension(parent.runsExtension);
    this.staticFilesExtension =
        new FlintStaticFilesExtension(
            plugin.getProject()); // TODO: Should static files be inherited too?
    this.selfInstallerExtension = new FlintSelfInstallerExtension(parent.selfInstallerExtension);

    this.type = parent.type;
    this.authors =
        parent.authors != null
            ? Arrays.copyOf(parent.authors, parent.authors.length)
            : new String[] {};
    this.flintVersion = parent.flintVersion;
    this.enablePublishing = parent.enablePublishing;
    this.autoConfigurePublishing = parent.autoConfigurePublishing;
  }

  /**
   * Overwrites the minecraft versions this project contains modules for.
   *
   * @param minecraftVersions The minecraft versions made available to this project
   * @deprecated Has been deprecated since we use yarn but also both mcp mappings.
   */
  @Deprecated
  public void minecraftVersions(String... minecraftVersions) {
    this.minecraftVersions.addAll(
        Arrays.stream(minecraftVersions)
            .map(
                minecraftVersion ->
                    new MinecraftVersion(minecraftVersion, EnvironmentType.MOD_CODER_PACK))
            .collect(Collectors.toCollection(CopyOnWriteArraySet::new)));
  }

  /**
   * Overwrites the minecraft versions this project contains modules for.
   *
   * @param minecraftVersions The minecraft versions made available to this project
   */
  public void minecraftModCoderPackVersions(String... minecraftVersions) {
    this.minecraftVersions.addAll(
        Arrays.stream(minecraftVersions)
            .map(
                minecraftVersion ->
                    new MinecraftVersion(minecraftVersion, EnvironmentType.MOD_CODER_PACK))
            .collect(Collectors.toList()));
  }

  /**
   * Overwrites the minecraft versions this project contains modules for.
   *
   * @param minecraftVersions The minecraft versions made available to this project
   */
  public void minecraftYarnVersions(String... minecraftVersions) {
    this.minecraftVersions.addAll(
        Arrays.stream(minecraftVersions)
            .map(minecraftVersion -> new MinecraftVersion(minecraftVersion, EnvironmentType.YARN))
            .collect(Collectors.toList()));
  }

  /**
   * Adds a minecraft to this project.
   *
   * @param version The minecraft version made available to this project.
   * @param type The environment type for this version.
   */
  public void minecraftVersion(String version, EnvironmentType type) {
    this.minecraftVersions.add(new MinecraftVersion(version, type));
  }

  /**
   * Retrieves the minecraft versions this project contains modules for.
   *
   * @return The minecraft versions made available to this project
   */
  public Set<MinecraftVersion> getMinecraftVersions() {
    return minecraftVersions;
  }

  /**
   * Overwrites the minecraft versions this project contains modules for.
   *
   * @param minecraftVersions The minecraft versions made available to this project
   */
  public void setMinecraftVersions(Set<MinecraftVersion> minecraftVersions) {
    this.minecraftVersions = minecraftVersions;
  }

  /**
   * Retrieves the flint version this project targets.
   *
   * @return The flint version this project targets
   */
  public String getFlintVersion() {
    return flintVersion;
  }

  /**
   * Overwrites the flint version this project targets.
   *
   * @param flintVersion The flint version this project targets
   */
  public void setFlintVersion(String flintVersion) {
    this.flintVersion = flintVersion;
  }

  /**
   * Adds a static file to this project configuration.
   *
   * @param from The path to get the file from
   * @param to The path to store the file to
   * @param upstreamName The name of the object in the repository
   * @deprecated Use the {@link #staticFiles(Action)} method instead.
   */
  @Deprecated
  public void staticFileEntry(Path from, Path to, String upstreamName) {
    Util.nagDeprecated(
        plugin.getProject(),
        "The staticFileEntry method of the flint extension is deprecated, use the staticFiles configuration instead");

    NamedDomainObjectContainer<FlintStaticFileDescription> staticFileDescriptions =
        this.staticFilesExtension.getStaticFileDescriptions();

    FlintStaticFileDescription description = staticFileDescriptions.create(upstreamName);
    description.from(from.toUri());
    description.to(to);
  }

  /**
   * Adds a static file to this project configuration.
   *
   * @param url The url to retrieve the file from
   * @param to The path to store the file to
   * @throws URISyntaxException If the URL can't be converted to an URI
   * @deprecated Use the {@link #staticFiles(Action)} method instead.
   */
  @Deprecated
  public void urlFileEntry(URL url, Path to) throws URISyntaxException {
    Util.nagDeprecated(
        plugin.getProject(),
        "The urlFileEntry method of the flint extension is deprecated, use the staticFiles configuration instead");

    NamedDomainObjectContainer<FlintStaticFileDescription> staticFileDescriptions =
        this.staticFilesExtension.getStaticFileDescriptions();

    FlintStaticFileDescription description =
        staticFileDescriptions.create(to.getFileName().toString());
    description.from(url.toURI());
    description.to(to);
  }

  /**
   * Retrieves the project filter predicate.
   *
   * @return The project filter predicate
   */
  public Predicate<Project> getProjectFilter() {
    return projectFilter;
  }

  /**
   * Overwrites the project filter with the given predicate. The project filter determines which sub
   * projects the plugin should automatically apply itself to.
   *
   * @param projectFilter The filter to test sub projects against
   */
  public void setProjectFilter(Predicate<Project> projectFilter) {
    this.projectFilter = projectFilter;
  }

  /**
   * Overwrites the project filter with the given predicate. The project filter determines which sub
   * projects the plugin should automatically apply itself to.
   *
   * @param projectFilter The filter to test sub projects against
   * @see #setProjectFilter(Predicate)
   */
  public void projectFilter(Predicate<Project> projectFilter) {
    setProjectFilter(projectFilter);
  }

  /**
   * Retrieves the project authors.
   *
   * @return The project authors
   */
  public String[] getAuthors() {
    return authors;
  }

  /**
   * Overwrites the project authors.
   *
   * @param authors The new project authors
   */
  public void setAuthors(String[] authors) {
    this.authors = authors;
  }

  /**
   * Overwrites the project authors.
   *
   * @param authors The new project authors
   */
  public void authors(String... authors) {
    setAuthors(authors);
  }

  /**
   * Retrieves the package type of this project.
   *
   * @return The package type of this project
   */
  public Type getType() {
    return type;
  }

  /**
   * Sets the package type of this project.
   *
   * @param type The new package type of this project
   */
  public void setType(@Nonnull Type type) {
    this.type = type;
  }

  /**
   * Retrieves the runs extension of this extension.
   *
   * @return The runs extension of this extension
   */
  public FlintRunsExtension getRuns() {
    return this.runsExtension;
  }

  /**
   * Configures the runs extension of this extension with the given action.
   *
   * @param action The action to use for configuration
   * @return The configured runs extension of this extension
   */
  public FlintRunsExtension runs(Action<FlintRunsExtension> action) {
    action.execute(this.runsExtension);
    return this.runsExtension;
  }

  /**
   * Retrieves the static files extension of this extension.
   *
   * @return The static files extension of this extension
   */
  public FlintStaticFilesExtension getStaticFiles() {
    return this.staticFilesExtension;
  }

  /**
   * Configures the static files extension of this extension with the given action.
   *
   * @param action The action to use for configuration
   * @return The configured static files extension of this extension
   */
  public FlintStaticFilesExtension staticFiles(
      Action<NamedDomainObjectContainer<FlintStaticFileDescription>> action) {
    action.execute(this.staticFilesExtension.getStaticFileDescriptions());
    return this.staticFilesExtension;
  }

  /**
   * Retrieves the self installer extension of this extension.
   *
   * @return The self installer extension of this extension
   */
  public FlintSelfInstallerExtension getSelfInstaller() {
    return this.selfInstallerExtension;
  }

  /**
   * Configures the self installer extension of this extension with the given action.
   *
   * @param action The action to use for configuration
   * @return The configured self installer extension of this extension
   */
  public FlintSelfInstallerExtension selfInstaller(Action<FlintSelfInstallerExtension> action) {
    action.execute(this.selfInstallerExtension);
    return this.selfInstallerExtension;
  }

  /**
   * Configures the values of this instance with the given closure.
   *
   * @param closure The closure to pass this instance to
   * @return Configured this
   * @throws IllegalStateException If the extension has been configured already
   */
  @Override
  @Nonnull
  public FlintGradleExtension configure(@Nonnull Closure closure) {
    if (configured) {
      throw new IllegalStateException("The flint extension can only be configured once");
    }

    FlintGradleExtension result = ConfigureUtil.configureSelf(closure, this);
    configured = true;
    plugin.onExtensionConfigured();
    return result;
  }

  /**
   * Triggers the {@link FlintGradlePlugin#onExtensionConfigured()} method. This method is meant to
   * be called from build scripts which need the extension to configure the plugin early without
   * changing values on the extension itself. This method may only be called if the extension has
   * not been configured by other means.
   *
   * @throws IllegalStateException If the extension has been configured already
   */
  public void configureNow() {
    if (configured) {
      throw new IllegalStateException(
          "Please only call configureNow() if you don't configure the extension by other means");
    }

    configured = true;
    plugin.onExtensionConfigured();
  }

  /**
   * Triggers the {@link FlintGradlePlugin#onExtensionConfigured()} method if the extension has not
   * been configured already.
   */
  public void ensureConfigured() {
    if (configured) {
      return;
    }

    configured = true;
    plugin.onExtensionConfigured();
  }

  /**
   * Sets the Authorization Bearer publish token used for authorizing at the lm-distributor.
   *
   * @param publishToken The Bearer token
   */
  public void publishToken(String publishToken) {
    this.publishToken = publishToken;
  }

  /**
   * Retrieves the Authorization Bearer publish token used for authorizing at the lm-distributor.
   *
   * @return The bearer token
   */
  public String getPublishToken() {
    return publishToken;
  }

  /**
   * Overwrites whether the publishing tasks should be created.
   *
   * @param enablePublishing If {@code true}, the plugin will create a few publish tasks
   */
  public void enablePublishing(boolean enablePublishing) {
    this.enablePublishing = enablePublishing;
  }

  /**
   * Determines whether the publish tasks should be created.
   *
   * @return {@code true} if the publish tasks should be created, {@code false} otherwise
   */
  public boolean shouldEnablePublishing() {
    return enablePublishing;
  }

  /**
   * Overwrites whether the {@link org.gradle.api.publish.PublishingExtension} should be
   * automatically configured if found.
   *
   * @param autoConfigurePublishing If {@code true}, the plugin will automatically set up publishing
   */
  public void autoConfigurePublishing(boolean autoConfigurePublishing) {
    this.autoConfigurePublishing = autoConfigurePublishing;
  }

  /**
   * Determines if the plugin should automatically configure the publishing extension.
   *
   * @return {@code true} if the plugin should automatically configure the extension, {@code false}
   *     otherwise
   */
  public boolean shouldAutoConfigurePublishing() {
    return autoConfigurePublishing;
  }

  public enum Type {
    LIBRARY,
    PACKAGE
  }
}
