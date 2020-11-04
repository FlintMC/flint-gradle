package net.flintmc.gradle.extension;

import groovy.lang.Closure;
import net.flintmc.gradle.FlintGradlePlugin;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Gradle extension block for configuring the plugin
 */
public class FlintGradleExtension implements Configurable<FlintGradleExtension> {
  public static final String NAME = "flint";

  private final FlintGradlePlugin plugin;
  private final FlintRunsExtension runsExtension;

  private boolean configured;

  private String[] authors;
  private String publishToken;
  private Set<String> minecraftVersions;
  private Predicate<Project> projectFilter;
  private boolean disableInternalSourceSet;
  private Type type = Type.PACKAGE;
  private String flintVersion;
  private Collection<FlintStaticFileEntry> staticFileEntries;

  /**
   * Creates a new {@link FlintGradleExtension} with default values.
   *
   * @param plugin The plugin this extension belongs to
   */
  public FlintGradleExtension(FlintGradlePlugin plugin) {
    System.out.println("NEW FLINT GRADLE EXTENSION " + plugin.getProject());
    this.plugin = plugin;

    this.minecraftVersions = new HashSet<>();
    this.projectFilter = p -> p.getPluginManager().hasPlugin("java");
    this.runsExtension = new FlintRunsExtension();
    this.staticFileEntries = new HashSet<>();
  }

  /**
   * Creates a new {@link FlintGradleExtension} with values copied from a parent
   * extension.
   *
   * @param plugin The plugin owning this extension
   * @param parent The parent extension
   */
  public FlintGradleExtension(FlintGradlePlugin plugin, FlintGradleExtension parent) {
    this.plugin = plugin;
    this.minecraftVersions = new HashSet<>(parent.minecraftVersions);
    this.projectFilter = parent.projectFilter;
    this.runsExtension = new FlintRunsExtension(parent.runsExtension);
    this.type = parent.type;
    this.authors = parent.authors != null ? Arrays.copyOf(parent.authors, parent.authors.length) : new String[]{};
    this.flintVersion = parent.flintVersion;
    this.staticFileEntries = new HashSet<>();

    parent.staticFileEntries.stream()
        .map(FlintStaticFileEntry::new)
        .forEach(this.staticFileEntries::add);
  }

  /**
   * Overwrites the minecraft versions this project contains modules for.
   *
   * @param minecraftVersions The minecraft versions made available to this project
   */
  public void minecraftVersions(String... minecraftVersions) {
    this.minecraftVersions = new HashSet<>();
    this.minecraftVersions.addAll(Arrays.asList(minecraftVersions));
  }

  /**
   * Retrieves the minecraft versions this project contains modules for.
   *
   * @return The minecraft versions made available to this project
   */
  public Set<String> getMinecraftVersions() {
    return minecraftVersions;
  }

  /**
   * Overwrites the minecraft versions this project contains modules for.
   *
   * @param minecraftVersions The minecraft versions made available to this project
   */
  public void setMinecraftVersions(Set<String> minecraftVersions) {
    this.minecraftVersions = minecraftVersions;
  }

  public void setFlintVersion(String flintVersion) {
    this.flintVersion = flintVersion;
  }

  public String getFlintVersion() {
    return flintVersion;
  }

  public void staticFileEntry(Path from, Path to, String upstreamName) {
    this.staticFileEntries.add(new FlintStaticFileEntry(from, to, upstreamName));
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
   * Overwrites the project filter with the given predicate. The project filter determines which sub projects
   * the plugin should automatically apply itself to.
   *
   * @param projectFilter The filter to test sub projects against
   * @see #setProjectFilter(Predicate)
   */
  public void projectFilter(Predicate<Project> projectFilter) {
    setProjectFilter(projectFilter);
  }

  public String[] getAuthors() {
    return authors;
  }

  public void setAuthors(String[] authors) {
    this.authors = authors;
  }

  /**
   * Overwrites the project filter with the given predicate. The project filter determines which sub projects
   * the plugin should automatically apply itself to.
   *
   * @param projectFilter The filter to test sub projects against
   */
  public void setProjectFilter(Predicate<Project> projectFilter) {
    this.projectFilter = projectFilter;
  }

  /**
   * Specifies if the internal source set should be disabled.
   *
   * @param disable If {@code true}, the plugin wont create an internal source set
   */
  public void setDisableInternalSourceSet(boolean disable) {
    this.disableInternalSourceSet = disable;
  }

  /**
   * Queries if the internal source set should be disabled.
   *
   * @return If the internal source set should be disabled
   */
  public boolean shouldDisableInternalSourceSet() {
    return this.disableInternalSourceSet;
  }

  public void setType(@Nonnull Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
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
   * Configures the runs extension of this extension with the given closure.
   *
   * @param closure The closure to use for configuration
   * @return The configured runs extension of this extension
   */
  public FlintRunsExtension runs(Action<FlintRunsExtension> closure) {
    closure.execute(this.runsExtension);
    return this.runsExtension;
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
   * Triggers the {@link FlintGradlePlugin#onExtensionConfigured()} method. This method is meant to be called from
   * build scripts which need the extension to configure the plugin early without changing values on the extension
   * itself. This method may only be called if the extension has not been configured by other means.
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
   * Triggers the {@link FlintGradlePlugin#onExtensionConfigured()} method if the extension has not been configured
   * already.
   */
  public void ensureConfigured() {
    if (configured) {
      return;
    }

    configured = true;
    plugin.onExtensionConfigured();
  }

  public Collection<FlintStaticFileEntry> getStaticFileEntries() {
    return staticFileEntries;
  }

  /**
   * Sets the Authorization Bearer publish token to authorize for publishment at the lm-distributor.
   *
   * @param publishToken The Bearer token
   */
  public void publishToken(String publishToken) {
    this.publishToken = publishToken;
  }

  /**
   * @return The Authorization Bearer publish token to authorize for publishment at the lm-distributor.
   */
  public String getPublishToken() {
    return publishToken;
  }

  public enum Type {
    LIBRARY, PACKAGE
  }


  public static class FlintStaticFileEntry {
    private Path to;
    private Path from;
    private String upstreamName;

    public FlintStaticFileEntry(FlintStaticFileEntry parent) {
      this.to = parent.to;
      this.from = parent.from;
      this.upstreamName = parent.upstreamName;
    }

    public FlintStaticFileEntry(Path from, Path to, String upstreamName) {
      this.from = from;
      this.to = to;
      this.setUpstreamName(upstreamName);
    }

    public void setFrom(Path from) {
      this.from = from;
    }

    public void setTo(Path to) {
      this.to = to;
    }

    public void setUpstreamName(String upstreamName) {
      if (upstreamName.isEmpty())
        throw new IllegalArgumentException("Upstream name must not be empty");
      if (!upstreamName.matches("^([a-zA-Z0-9)]|\\.||_|-)+$"))
        throw new IllegalArgumentException("Can only use 'a-z', 'A-Z', '0-9', '_', '-' and '.'] in upstream name");
      this.upstreamName = upstreamName;
    }

    public Path getFrom() {
      return from;
    }

    public Path getTo() {
      return to;
    }

    public String getUpstreamName() {
      return upstreamName;
    }
  }

}
