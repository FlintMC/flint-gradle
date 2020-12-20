package net.flintmc.gradle.support;

import groovy.lang.Closure;
import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.util.JavaClosure;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionContainer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Helper for automatically generating the delegation properties on the dependency extensions based on the Kotlin
 * extensions.
 *
 * @see org.gradle.kotlin.dsl.DependencyHandlerExtensionsKt
 */
public class GroovyDependencyHandlerExtensions {
  /**
   * Installs the dependency handler extensions for the given project.
   *
   * @param project The project to install the extensions on
   */
  public static void install(Project project) {
    try {
      ExtensionContainer dependencyExtensions = project.getDependencies().getExtensions();
      Class<?> kotlinExtensions = Class.forName("FlintDependencyHandlerExtensionsKt");

      for(Method method : kotlinExtensions.getMethods()) {
        if(
          // Ignore if added already due to overload
            dependencyExtensions.findByName(method.getName()) != null ||
                // Ignore methods which are not static or public
                (method.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) == 0
        ) {
          continue;
        }

        // Add the extensions
        dependencyExtensions.add(method.getName(), new Closure<Void>(GroovyDependencyHandlerExtensions.class) {
          @SuppressWarnings("unused") // Called by groovy
          public Object doCall(Object... args) {
            // Construct arguments suitable for calling the kotlin extensions
            Object[] newArgs = new Object[args.length + 1];
            newArgs[0] = project.getDependencies();
            System.arraycopy(args, 0, newArgs, 1, args.length);

            // Invoke now
            return InvokerHelper.invokeMethod(kotlinExtensions, method.getName(), newArgs);
          }
        });
      }
    } catch(Exception e) {
      throw new FlintGradleException("Internal error", e);
    }
  }
}
