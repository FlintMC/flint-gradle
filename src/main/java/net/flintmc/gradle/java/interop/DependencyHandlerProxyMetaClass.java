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

import groovy.lang.GroovyObject;
import groovy.lang.MetaClassImpl;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;

/**
 * Helper class because Groovy does not properly work with Java proxies.
 */
@SuppressWarnings({"unchecked", "RedundantSuppression"}) // The Java compiler emits warnings IDE's don't detect
public class DependencyHandlerProxyMetaClass extends MetaClassImpl {
  private final ConfigurationContainer configurationContainer;
  private final DependencyHandlerProxy proxy;

  /**
   * Constructs a new {@link DependencyHandlerProxyMetaClass}.
   *
   * @param proxied                The proxied dependency handler instance
   * @param configurationContainer The configuration container
   * @param proxy                  The proxy that is being used
   */
  public DependencyHandlerProxyMetaClass(
      DependencyHandler proxied,
      ConfigurationContainer configurationContainer,
      DependencyHandlerProxy proxy
  ) {
    super(proxied.getClass());
    this.configurationContainer = configurationContainer;
    this.proxy = proxy;
    initialize();
  }

  @Override
  public Object invokeMethod(
      Class sender,
      Object object,
      String methodName,
      Object[] originalArguments,
      boolean isCallToSuper,
      boolean fromInsideClass
  ) {
    String versionedConfigurationName = proxy.toVersionedConfigurationName(methodName);
    if(configurationContainer.findByName(versionedConfigurationName) != null) {
      // Found a versioned configuration, replace the method name and pass through
      methodName = versionedConfigurationName;
    } else if(configurationContainer.findByName(methodName) != null) {
      // There is a configuration with the original method name, but it has no versioned counterpart
      throw new IllegalArgumentException("Can't add dependencies to configuration " + methodName +
          " without a versioned counterpart in a versioned block");
    }

    // Delegate execution to the original object
    return ((GroovyObject) proxy.getUnderlying()).invokeMethod(methodName, originalArguments);
  }
}
