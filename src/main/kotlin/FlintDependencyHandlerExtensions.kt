import net.flintmc.gradle.java.VersionedDependencyAdder
import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope

/**
 * Configures the dependencies for a specific minecraft version.
 *
 * @param version The version to configure the dependencies for
 * @param config The handler to use for configuration
 *
 * @see [VersionedDependencyAdder.accept]
 */
fun DependencyHandlerScope.version(version: String, config: DependencyHandlerScope.() -> Unit) = apply {
    (extensions.getByName("versionedDependencyAdder") as VersionedDependencyAdder).accept(version) {
        DependencyHandlerScope.of(this).invoke(config)
    }
}

/**
 * Configures the dependencies for a specific minecraft version.
 *
 * @param version The version to configure the dependencies for
 * @param config The handler to use for configuration
 *
 * @see [VersionedDependencyAdder.accept]
 */
fun DependencyHandler.version(version: String, config: Action<DependencyHandler>) = apply {
    (extensions.getByName("versionedDependencyAdder") as VersionedDependencyAdder).accept(version, config)
}
