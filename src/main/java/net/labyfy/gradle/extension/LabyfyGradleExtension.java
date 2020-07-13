package net.labyfy.gradle.extension;

import groovy.lang.Closure;
import net.labyfy.gradle.LabyfyGradlePlugin;
import org.gradle.api.Project;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Gradle extension block for configuring the plugin
 */
public class LabyfyGradleExtension implements Configurable<LabyfyGradleExtension> {
    public static final String NAME = "labyfy";

    private final LabyfyGradlePlugin plugin;
    private final LabyfyRunsExtension runsExtension;

    private boolean configured;

    private String publishToken;
    private Set<String> minecraftVersions;
    private Predicate<Project> projectFilter;
    private boolean disableInternalSourceSet;

    /**
     * Creates a new {@link LabyfyGradleExtension} with default values.
     *
     * @param plugin The plugin this extension belongs to
     */
    public LabyfyGradleExtension(LabyfyGradlePlugin plugin) {
        this.plugin = plugin;

        this.minecraftVersions = new HashSet<>();
        this.projectFilter = p -> p.getPluginManager().hasPlugin("java");
        this.runsExtension = new LabyfyRunsExtension();
    }

    /**
     * Creates a new {@link LabyfyGradleExtension} with values copied from a parent
     * extension.
     *
     * @param plugin The plugin owning this extension
     * @param parent The parent extension
     */
    public LabyfyGradleExtension(LabyfyGradlePlugin plugin, LabyfyGradleExtension parent) {
        this.plugin = plugin;
        this.minecraftVersions = new HashSet<>(parent.minecraftVersions);
        this.projectFilter = parent.projectFilter;
        this.runsExtension = new LabyfyRunsExtension(parent.runsExtension);
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

    /**
     * Retrieves the runs extension of this extension.
     *
     * @return The runs extension of this extension
     */
    public LabyfyRunsExtension getRuns() {
        return this.runsExtension;
    }

    /**
     * Configures the runs extension of this extension with the given closure.
     *
     * @param closure The closure to use for configuration
     * @return The configured runs extension of this extension
     */
    public LabyfyRunsExtension runs(Closure<?> closure) {
        return this.runsExtension.configure(closure);
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
    public LabyfyGradleExtension configure(@Nonnull Closure closure) {
        if (configured) {
            throw new IllegalStateException("The labyfy extension can only be configured once");
        }

        LabyfyGradleExtension result = ConfigureUtil.configureSelf(closure, this);
        configured = true;
        plugin.onExtensionConfigured();
        return result;
    }

    /**
     * Triggers the {@link LabyfyGradlePlugin#onExtensionConfigured()} method. This method is meant to be called from
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
     * Triggers the {@link LabyfyGradlePlugin#onExtensionConfigured()} method if the extension has not been configured
     * already.
     */
    public void ensureConfigured() {
        if (configured) {
            return;
        }

        configured = true;
        plugin.onExtensionConfigured();
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
}
