package net.flintmc.gradle.minecraft;

import groovy.util.slurpersupport.GPathResult;

/**
 * Transformer for log4j XML config files
 */
public interface LogConfigTransformer {
  /**
   * Transforms the given XML node.
   *
   * @param configuration The configuration for which the log file is being created
   * @param node          The XML to transform
   */
  void transform(String configuration, GPathResult node);
}
