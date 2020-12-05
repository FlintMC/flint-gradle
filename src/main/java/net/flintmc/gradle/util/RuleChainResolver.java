package net.flintmc.gradle.util;

import net.flintmc.gradle.minecraft.data.version.*;
import org.apache.tools.ant.taskdefs.condition.Os;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public class RuleChainResolver {
  // Collect static values which appear in multiple rules
  private static final VersionedOsRuleValue RULE_OS_NAME;
  private static final String RULE_OS_VERSION;
  private static final String RULE_OS_ARCH;

  static {
    // Convert the OS family to a usable value
    if (Os.isFamily(Os.FAMILY_MAC)) {
      RULE_OS_NAME = VersionedOsRuleValue.OSX;
    } else if (Os.isFamily(Os.FAMILY_WINDOWS)) {
      RULE_OS_NAME = VersionedOsRuleValue.WINDOWS;
    } else if (Os.isFamily(Os.FAMILY_UNIX)) {
      RULE_OS_NAME = VersionedOsRuleValue.LINUX;
    } else {
      RULE_OS_NAME = VersionedOsRuleValue.OTHER;
    }

    // Those seem to be taken directly from the system properties
    RULE_OS_VERSION = System.getProperty("os.version");
    RULE_OS_ARCH = System.getProperty("os.arch");
  }

  /**
   * Tests if the given rule chain applies to the current environment.
   *
   * @param rules The rule chain to test
   * @return {@code true} if the rule chain matches or is {@code null}, false otherwise
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public static boolean testRuleChain(List<VersionedRule> rules) {
    return testRuleChain(rules, Collections.emptyMap());
  }

  /**
   * Tests if the given rule chain applies to the current environment.
   *
   * @param rules    The rule chain to test
   * @param features The features to use for detection
   * @return {@code true} if the rule chain matches or is {@code null}, false otherwise
   */
  public static boolean testRuleChain(List<VersionedRule> rules, Map<String, Object> features) {
    if (rules == null) {
      return true;
    }

    boolean allow = false;

    for (VersionedRule rule : rules) {
      boolean applies = true;

      // First check if the OS matches
      if (rule.getOs() != null) {
        // We got an OS rule
        VersionedOsRule osRule = rule.getOs();

        if (osRule.getName() != null) {
          // If the OS name is not null, check if it matches the current OS
          applies = osRule.getName() == RULE_OS_NAME;
        }

        if (applies && osRule.getVersion() != null) {
          // Then check the OS version, this seems to be a Regex in the Json
          Pattern versionPattern = Pattern.compile(osRule.getVersion());
          applies = versionPattern.matcher(RULE_OS_VERSION).matches();
        }

        if (applies && osRule.getArch() != null) {
          // Also check the arch, this check is kind of fragile, but it also is not
          // used for important rules
          applies = RULE_OS_ARCH.equals(osRule.getArch());
        }
      }

      if (applies && rule.getFeatures() != null) {
        // Last check for features
        for (String key : rule.getFeatures().keySet()) {
          // This would also allow null keys
          // Since there is no documentation about it, simply assume that equal means match, even
          // if both are null
          applies = Objects.equals(features.get(key), rule.getFeatures().get(key));
          if (!applies) {
            break;
          }
        }
      }

      if (applies) {
        // If the rule applies, take the selected action
        allow = rule.getAction() == VersionedRuleAction.ALLOW;
      }
    }

    return allow;
  }

  /**
   * Resolves the matching native classifier of the given library for the current environment.
   *
   * @param library The library to resolve the classifier of
   * @return The resolved classifier, or {@code null} if no classifier matches
   */
  public static String resolveNativeClassifier(VersionedLibrary library) {
    if (library.getNatives() == null) {
      return null;
    }

    String nativesName = RULE_OS_NAME.name().toLowerCase();
    return library.getNatives().get(nativesName);
  }
}
