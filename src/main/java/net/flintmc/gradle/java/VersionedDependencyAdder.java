package net.flintmc.gradle.java;

import org.gradle.api.Action;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.util.function.BiConsumer;

/**
 * Utility class for adding dependencies using a versioned name
 */
public class VersionedDependencyAdder implements BiConsumer<String, Action<DependencyHandler>> {
  @Override
  public void accept(String version, Action<DependencyHandler> dependencyHandlerAction) {
    System.out.println("Adding dependencies on " + version + " using " + dependencyHandlerAction);
  }
}
