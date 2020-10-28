package net.flintmc.gradle.java;

import groovy.lang.Closure;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.util.Map;
import java.util.Objects;

/**
 * Utility class for adding dependencies using a versioned name
 */
public class VersionedDependencyAdder extends Closure<Dependency> {
    private final String suffix;
    private final DependencyHandler dependencyHandler;

    /**
     * Constructs a new {@link VersionedDependencyAdder} with the given owner, prefix and configurations.
     *
     * @param owner             The owner of this closure
     * @param suffix            The suffix to apply to all configurations before retrieving them
     * @param dependencyHandler The dependency handler to add dependencies on
     */
    public VersionedDependencyAdder(Object owner, String suffix, DependencyHandler dependencyHandler) {
        super(owner);
        this.suffix = suffix;
        this.dependencyHandler = dependencyHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dependency call(Object... args) {
        if (args.length < 2) {
            // Can't work with less than 3 arguments
            throw new IllegalArgumentException("Required at least version and the dependency to configure");
        } else if (args.length == 2 || (args.length == 3 && args[2] instanceof Closure)) {
            // Extract required properties
            String version;
            Object dependencyNotation;
            Closure<?> configurationClosure;

            if (args.length == 3) {
                // We have a configuration closure
                configurationClosure = (Closure<?>) args[2];
            } else {
                configurationClosure = null;
            }

            if (args[0] instanceof Map) {
                // If the first argument is a map, the function has been called with the dependency notation as a map
                dependencyNotation = args[0];
                version = "v" + Objects.requireNonNull(args[1], "The version may not be null")
                    .toString().replace('.', '_');
            } else {
                // The dependencies are given in normal order
                version = "v" + Objects.requireNonNull(args[0], "The version may not be null")
                    .toString().replace('.', '_');
                dependencyNotation = Objects.requireNonNull(args[1], "Dependency may not be null").toString();
            }

            if (configurationClosure == null) {
                // Invoke the handler with a dependency configuration closure
                return dependencyHandler.add(version + suffix, dependencyNotation);
            }

            // Invoke the handler without a dependency configuration closure
            return dependencyHandler.add(version + suffix, dependencyNotation, configurationClosure);
        } else {
            // Got a lot of dependency objects, process all of them
            // The first parameter is always the version
            String configuration =
                Objects.requireNonNull(args[0], "The version may not be null").toString() + suffix;

            for (int i = 1; i < args.length; i++) {
                // Add every single dependency item
                dependencyHandler.add(configuration,
                    Objects.requireNonNull(args[i], "The dependency arguments may not be null").toString());
            }

            return null;
        }
    }
}
