import net.flintmc.gradle.java.VersionedDependencyAdder
import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope

/*
* This file contains the extension functions which can be called on the dependency handler using Kotlin.
* The class net.flintmc.gradle.support.GroovyDependencyHandlerExtensions will automatically generate the
* extension functions on the DependencyHandler instance of the project based on the methods it finds in
* this class (FlintDependencyHandlerExtensionsKt).
*
* In order to avoid having to import this class, it is in the default package.
*/

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
