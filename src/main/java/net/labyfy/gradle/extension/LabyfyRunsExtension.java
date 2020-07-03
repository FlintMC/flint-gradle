package net.labyfy.gradle.extension;

import groovy.lang.Closure;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

import javax.annotation.Nonnull;
import java.util.*;

public class LabyfyRunsExtension implements Configurable<LabyfyRunsExtension> {
    private final Set<String> allIncludedConfigurations;
    private final Map<String, Set<String>> includedSourceSets;
    private final Map<String, Set<String>> excludedSourceSets;

    /**
     * Constructs a {@link LabyfyRunsExtension} with default values.
     */
    public LabyfyRunsExtension() {
        this.allIncludedConfigurations = new HashSet<>();
        this.allIncludedConfigurations.add("main");
        this.includedSourceSets = new HashMap<>();
        this.excludedSourceSets = new HashMap<>();
    }

    /**
     * Constructs a {@link LabyfyRunsExtension} copying from the given parent extension.
     *
     * @param parent The parent to copy values from
     */
    public LabyfyRunsExtension(LabyfyRunsExtension parent) {
        this.allIncludedConfigurations = new HashSet<>(parent.allIncludedConfigurations);
        this.includedSourceSets = new HashMap<>(parent.includedSourceSets);
        this.excludedSourceSets = new HashMap<>(parent.excludedSourceSets);
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
     * @param configuration The name of the run configuration to be included in
     * @param versionOrSourceSets The source sets or versions to include
     */
    public void include(String configuration, String... versionOrSourceSets) {
        List<String> versionList = Arrays.asList(versionOrSourceSets);

        // Add the configurations to the list
        this.includedSourceSets.computeIfAbsent(configuration, (k) -> new HashSet<>()).addAll(versionList);

        // Probe for exclusion from the list
        Set<String> excludedConfigurations = this.excludedSourceSets.get(configuration);
        if(excludedConfigurations != null) {
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
     * @param configuration Tne name of the run configuration to exclude the source sets or versions from
     * @param sourceSetsOrVersions The source sets or versions to exclude
     */
    public void exclude(String configuration, String... sourceSetsOrVersions) {
        List<String> versionList = Arrays.asList(sourceSetsOrVersions);

        // Add the configuration to the list
        this.excludedSourceSets.computeIfAbsent(configuration, (k) -> new HashSet<>()).addAll(versionList);

        // Probe for inclusions of the list
        Set<String> versionedConfigurations = this.includedSourceSets.get(configuration);
        if(versionedConfigurations != null) {
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
     * Configures this runs extension with the given closure.
     *
     * @param closure The closure to use for configuration
     * @return this
     */
    @Override
    @Nonnull
    public LabyfyRunsExtension configure(@Nonnull Closure closure) {
        return ConfigureUtil.configureSelf(closure, this);
    }
}
