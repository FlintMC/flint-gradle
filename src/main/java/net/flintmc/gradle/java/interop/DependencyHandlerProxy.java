/*
 * FlintMC
 * Copyright (C) 2020-2021 LabyMedia GmbH and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package net.flintmc.gradle.java.interop;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.util.Configurable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        collectAllInterfaces(underlying).toArray(new Class<?>[0]),
        proxy
    );
  }

  /**
   * Collects all interfaces classes of the given object.
   *
   * @param o The object to scan for interface
   * @return A set of all found interfaces
   */
  private static Set<Class<?>> collectAllInterfaces(Object o) {
    Set<Class<?>> toScan = new HashSet<>();
    toScan.add(o.getClass());

    Set<Class<?>> found = new HashSet<>();

    while(!toScan.isEmpty()) {
      // Clone and clear interfaces which still need to be scanned
      Set<Class<?>> scanning = new HashSet<>(toScan);
      toScan.clear();

      for(Class<?> clazz : scanning) {
        if(found.contains(clazz)) {
          // Interface scanned already
          continue;
        }

        if(clazz.isInterface()) {
          // Index the currently scanned interface
          found.add(clazz);
        }

        // Add all found interfaces
        toScan.addAll(Arrays.asList(clazz.getInterfaces()));

        Class<?> superClass = clazz.getSuperclass();
        if(superClass != Object.class && superClass != null) {
          // There are more superclasses except Object to scan
          toScan.add(superClass);
        }
      }
    }

    return found;
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
      return metaClass(proxy);
    } else if(method.getName().equals("add")) {
      // Modify the arguments for the 'add' method by prepending the versioned configuration name
      String configuration = (String) args[0];
      if(configuration.length() > 1) {
        // Construct the new configuration name, eg. 'v1_15_2Implementation'
        args[0] = toVersionedConfigurationName(configuration);
      }
    } else if(method.getName().equals("invokeMethod")) {
      // Explicit Method invocations need to be delegated to the meta proxy
      Object[] newArgs = new Object[args.length - 1];
      System.arraycopy(args, 1, newArgs, 0, newArgs.length);
      return metaClass(proxy).invokeMethod(underlying, (String) args[0], newArgs);
    }

    return method.invoke(underlying, args);
  }

  /**
   * Retrieves or creates the meta class if necessary.
   *
   * @param proxy The proxy to create the meta class for
   * @return The cached or created meta class
   */
  private DependencyHandlerProxyMetaClass metaClass(Object proxy) {
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
