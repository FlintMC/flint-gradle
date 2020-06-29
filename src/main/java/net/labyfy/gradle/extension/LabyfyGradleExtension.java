package net.labyfy.gradle.extension;

import groovy.lang.Closure;
import net.labyfy.gradle.LabyfyGradlePlugin;
import org.gradle.api.Project;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Gradle extension block for configuring the plugin
 */
public class LabyfyGradleExtension implements Configurable<LabyfyGradleExtension> {
    public static final String NAME = "labyfy";

    private final LabyfyGradlePlugin plugin;

    private boolean configured;

    private List<String> minecraftVersions;
    private Predicate<Project> projectFilter;
    private boolean disableInternalSourceSet;

    /**
     * Creates a new {@link LabyfyGradleExtension} with default values
     *
     * @param plugin The plugin this extension belongs to
     */
    @Inject
    public LabyfyGradleExtension(LabyfyGradlePlugin plugin) {
        this.plugin = plugin;

        this.minecraftVersions = new ArrayList<>();
        this.projectFilter = p -> p.getPluginManager().hasPlugin("java");
    }

    /**
     * Overwrites the minecraft versions this project contains modules for.
     *
     * @param minecraftVersions The minecraft versions made available to this project
     */
    public void minecraftVersions(String... minecraftVersions) {
        this.minecraftVersions = new ArrayList<>();
        this.minecraftVersions.addAll(Arrays.asList(minecraftVersions));
    }

    /**
     * Overwrites the minecraft versions this project contains modules for.
     *
     * @param minecraftVersions The minecraft versions made available to this project
     */
    public void setMinecraftVersions(List<String> minecraftVersions) {
        this.minecraftVersions = minecraftVersions;
    }

    /**
     * Retrieves the minecraft versions this project contains modules for.
     *
     * @return The minecraft versions made available to this project
     */
    public List<String> getMinecraftVersions() {
        return minecraftVersions;
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
     * Configures the values of this instance with the given closure.
     *
     * @param closure The closure to pass this instance to
     * @return Configured this
     */
    @Override
    @Nonnull
    public LabyfyGradleExtension configure(@Nonnull Closure closure) {
        if(configured) {
            throw new IllegalStateException("The labyfy extension can only be configured once");
        }

        LabyfyGradleExtension result = ConfigureUtil.configureSelf(closure, this);
        configured = true;
        plugin.onExtensionConfigured();
        return result;
    }
}
