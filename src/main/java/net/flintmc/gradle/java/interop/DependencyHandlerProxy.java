package net.flintmc.gradle.java.interop;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Proxy for the {@link DependencyHandler#add(String, Object)} and {@link DependencyHandler#add(String, Object,
 * Closure)} calls for versioned configuration redirection.
 */
public class DependencyHandlerProxy implements InvocationHandler {
  /**
   * Creates a new proxy of the given dependency handler for the given minecraft version.
   *
   * @param underlying             The dependency handler to proxy
   * @param configurationContainer The configuration container to resolve configurations from
   * @param minecraftVersion       The minecraft version to create the proxy for
   * @return The proxied dependency handler
   */
  public static DependencyHandler of(
      DependencyHandler underlying, ConfigurationContainer configurationContainer, String minecraftVersion) {
    String prefix = 'v' + minecraftVersion.replace('.', '_');

    if(Proxy.isProxyClass(underlying.getClass())) {
      // The object has been proxied already, check if it is a dependency handler proxy
      InvocationHandler handler = Proxy.getInvocationHandler(underlying);

      if(handler instanceof DependencyHandlerProxy) {
        // The object has already been proxied with a dependency handler proxy, and we don't want to proxy it twice,
        // so we extract the underlying instance
        underlying = ((DependencyHandlerProxy) handler).underlying;
      }
    }

    // Construct the new proxy
    DependencyHandlerProxy proxy = new DependencyHandlerProxy(underlying, configurationContainer, prefix);

    return (DependencyHandler) Proxy.newProxyInstance(
        DependencyHandler.class.getClassLoader(),
        new Class<?>[]{DependencyHandler.class, GroovyObject.class},
        proxy
    );
  }

  private final DependencyHandler underlying;
  private final ConfigurationContainer configurationContainer;
  private final String configurationPrefix;

  private DependencyHandlerProxyMetaClass cachedMetaClass;

  /**
   * Constructs a new {@link DependencyHandlerProxy} for the given {@link DependencyHandler}.
   *
   * @param underlying             The underlying, non proxied dependency handler
   * @param configurationContainer The configuration container to add configurations to
   * @param configurationPrefix    The prefix to prepend to add calls
   */
  private DependencyHandlerProxy(
      DependencyHandler underlying, ConfigurationContainer configurationContainer, String configurationPrefix) {
    this.underlying = underlying;
    this.configurationContainer = configurationContainer;
    this.configurationPrefix = configurationPrefix;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if(method.equals(GroovyObject.class.getMethod("getMetaClass"))) {
      // Override meta class
      if(cachedMetaClass == null) {
        // No meta class yet, create a new one
        cachedMetaClass = new DependencyHandlerProxyMetaClass(
            (DependencyHandler) proxy,
            configurationContainer,
            this
        );
      }

      return cachedMetaClass;
    } else if(method.getName().equals("add")) {
      // Modify the arguments for the 'add' method by prepending the versioned configuration name
      String configuration = (String) args[0];
      if(configuration.length() > 1) {
        // Construct the new configuration name, eg. 'v1_15_2Implementation'
        args[0] = toVersionedConfigurationName(configuration);
      }
    }

    return method.invoke(underlying, args);
  }

  /**
   * Transforms the given configuration name into a configuration name of the versioned configuration.
   *
   * @param configurationName The configuration name to transform
   * @return The transformed, now versioned configuration name
   */
  public String toVersionedConfigurationName(String configurationName) {
    return configurationPrefix + Character.toUpperCase(configurationName.charAt(0)) + configurationName.substring(1);
  }

  /**
   * Retrieves the underlying non-proxied handler.
   *
   * @return The underlying non-proxied handler
   */
  public DependencyHandler getUnderlying() {
    return underlying;
  }
}
