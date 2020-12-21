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
