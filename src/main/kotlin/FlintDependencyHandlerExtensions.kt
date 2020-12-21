import groovy.lang.Closure
import net.flintmc.gradle.java.interop.FlintDependencyAdder
import org.gradle.api.artifacts.Dependency
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
 * @see [FlintDependencyAdder.accept]
 */
fun DependencyHandlerScope.minecraft(version: String, config: DependencyHandlerScope.() -> Unit) = apply {
    extensions.getByType<FlintDependencyAdder>().accept(version) {
        DependencyHandlerScope.of(this).invoke(config)
    }
}

/**
 * Configures the dependencies for a specific set of minecraft versions.
 *
 * @param version The versions to configure the dependencies for
 * @param config The handler to use for configuration
 *
 * @see [FlintDependencyAdder.accept]
 */
fun DependencyHandlerScope.minecraft(vararg version: String, config: DependencyHandlerScope.() -> Unit) = apply {
    version.forEach {
        extensions.getByType<FlintDependencyAdder>().accept(it) {
            DependencyHandlerScope.of(this).invoke(config)
        }
    }
}

/**
 * Creates a flint API dependency for the given module.
 *
 * @param module The module to create the dependency for
 */
fun DependencyHandlerScope.flintApi(module: String): Dependency =
    this.create("net.flintmc:$module:${extensions.getByType<FlintDependencyAdder>().flintVersion}")

// ================= Groovy compatible extensions ================= //

/**
 * Configures the dependencies for a specific minecraft version.
 *
 * @param version The version to configure the dependencies for
 * @param config The handler to use for configuration
 *
 * @see [FlintDependencyAdder.accept]
 */
fun DependencyHandler.minecraft(version: String, config: Closure<Void>) = apply {
    extensions.getByType<FlintDependencyAdder>().accept(version) {
        config.boundCall(this)
    }
}

/**
 * Configures the dependencies for a specific set of minecraft versions.
 *
 * @param version The versions to configure the dependencies for
 * @param config The handler to use for configuration
 *
 * @see [FlintDependencyAdder.accept]
 */
fun DependencyHandler.minecraft(version: List<String>, config: Closure<Void>) = apply {
    version.forEach {
        extensions.getByType<FlintDependencyAdder>().accept(it) {
            config.boundCall(this)
        }
    }
}

/**
 * Creates a flint API dependency for the given module.
 *
 * @param module The module to create the dependency for
 */
fun DependencyHandler.flintApi(module: String): Dependency =
    this.create("net.flintmc:$module:${extensions.getByType<FlintDependencyAdder>().flintVersion}")

/**
 * Calls a [Closure] bound with the given object resolving all properties accessed in the closure
 * on the bound first before continuing with outer scopes.
 *
 * @param bound The object to call the closure with
 */
private fun <T> Closure<T>.boundCall(bound: Any?): T? {
    delegate = bound
    resolveStrategy = Closure.DELEGATE_FIRST
    return call(bound)
}
