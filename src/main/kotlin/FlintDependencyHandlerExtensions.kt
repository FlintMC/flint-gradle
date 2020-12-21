import net.flintmc.gradle.java.interop.VersionedDependencyAdder
import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.getByType

/*
* This file contains the extension functions which can be called on the dependency handler using Kotlin.
* The class net.flintmc.gradle.support.GroovyDependencyHandlerExtensions will automatically generate the
* extension functions on the DependencyHandler instance of the project based on the methods it finds in
* this class (FlintDependencyHandlerExtensionsKt).
*
* In order to avoid having to import this class, it is in the default package.
*/

// ================= Kotlin compatible extensions ================= //

/**
 * Configures the dependencies for a specific minecraft version.
 *
 * @param version The version to configure the dependencies for
 * @param config The handler to use for configuration
 *
 * @see [VersionedDependencyAdder.accept]
 */
fun DependencyHandlerScope.minecraft(version: String, config: DependencyHandlerScope.() -> Unit) = apply {
    (extensions.getByName("versionedDependencyAdder") as VersionedDependencyAdder).accept(version) {
        DependencyHandlerScope.of(this).invoke(config)
    }
}

/**
 * Configures the dependencies for a specific set of minecraft versions.
 *
 * @param version The versions to configure the dependencies for
 * @param config The handler to use for configuration
 *
 * @see [VersionedDependencyAdder.accept]
 */
fun DependencyHandlerScope.minecraft(vararg version: String, config: DependencyHandlerScope.() -> Unit) = apply {
    version.forEach {
        extensions.getByType<VersionedDependencyAdder>().accept(it) {
            DependencyHandlerScope.of(this).invoke(config)
        }
    }
}

// ================= Groovy compatible extensions ================= //

/**
 * Configures the dependencies for a specific minecraft version.
 *
 * @param version The version to configure the dependencies for
 * @param config The handler to use for configuration
 *
 * @see [VersionedDependencyAdder.accept]
 */
fun DependencyHandler.minecraft(version: String, config: Action<DependencyHandler>) = apply {
    extensions.getByType<VersionedDependencyAdder>().accept(version, config)
}

/**
 * Configures the dependencies for a specific set of minecraft versions.
 *
 * @param version The versions to configure the dependencies for
 * @param config The handler to use for configuration
 *
 * @see [VersionedDependencyAdder.accept]
 */
fun DependencyHandler.minecraft(version: List<String>, config: Action<DependencyHandler>) = apply {
    version.forEach {
        extensions.getByType<VersionedDependencyAdder>().accept(it, config)
    }
}
