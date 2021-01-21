package net.flintmc.gradle.extension;

import groovy.lang.Closure;
import net.flintmc.gradle.maven.FlintResolutionStrategy;
import org.gradle.util.Configurable;
import org.gradle.util.ConfigureUtil;

public class FlintResolutionStrategyExtension
    implements Configurable<FlintResolutionStrategyExtension> {

  public void forceDependency(Object moduleVersionSelectorNotation) {
    FlintResolutionStrategy.getInstance()
        .forceDependency(moduleVersionSelectorNotation);
  }

  public void forceDependencies(Object... moduleVersionSelectorNotations) {
    FlintResolutionStrategy.getInstance()
        .forceDependencies(moduleVersionSelectorNotations);
  }

  @Override
  public FlintResolutionStrategyExtension configure(Closure closure) {
    return ConfigureUtil.configure(closure, this);
  }
}
