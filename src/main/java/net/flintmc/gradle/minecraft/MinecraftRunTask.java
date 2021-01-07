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

package net.flintmc.gradle.minecraft;

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.minecraft.data.version.ArgumentString;
import net.flintmc.gradle.minecraft.data.version.VersionedArguments;
import net.flintmc.gradle.minecraft.ui.LoginDialog;
import net.flintmc.gradle.minecraft.ui.LoginDialogResult;
import net.flintmc.gradle.minecraft.yggdrasil.YggdrasilAuthenticationException;
import net.flintmc.gradle.minecraft.yggdrasil.YggdrasilAuthenticator;
import net.flintmc.gradle.property.FlintPluginProperties;
import net.flintmc.gradle.util.RuleChainResolver;
import net.flintmc.gradle.util.Util;
import org.gradle.api.tasks.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Executes a minecraft version with all the required setup.
 */
public class MinecraftRunTask extends JavaExec {
  // Variables used for launch arguments
  private static final Map<String, String> DEFAULT_VARIABLES;

  static {
    DEFAULT_VARIABLES = new HashMap<>();
    DEFAULT_VARIABLES.put("auth_player_name", "FlintGradle");
    DEFAULT_VARIABLES.put("auth_uuid", "cb3f34da-cd1a-420c-8616-655204b5e004");
    DEFAULT_VARIABLES.put("auth_access_token", "0");
    DEFAULT_VARIABLES.put("user_type", "mojang");
    DEFAULT_VARIABLES.put("launcher_name", "flint-gradle");
    DEFAULT_VARIABLES.put("launcher_version", "3.0.0");
  }

  private String configurationName;
  private String version;
  private String versionType;
  private VersionedArguments versionedArguments;
  private PotentialMinecraftClasspath potentialClasspath;
  private String assetIndex;
  private Path assetsPath;
  private Path nativesDirectory;
  private YggdrasilAuthenticator authenticator;


  /**
   * Sets the name of the configuration this run task belongs to.
   *
   * @param configurationName The name of the configuration owning this task
   */
  public void setConfigurationName(String configurationName) {
    this.configurationName = configurationName;
  }

  /**
   * Sets the minecraft version that is being executed
   *
   * @param version The minecraft version that is being executed
   */
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * Sets the version type passed to minecraft.
   *
   * @param versionType The version type passed to minecraft
   */
  public void setVersionType(String versionType) {
    this.versionType = versionType;
  }

  /**
   * Sets the arguments used to run minecraft.
   *
   * @param versionedArguments The arguments used to run minecraft
   */
  public void setVersionedArguments(VersionedArguments versionedArguments) {
    this.versionedArguments = versionedArguments;
  }

  /**
   * Retrieves the potential classpath.
   *
   * @return The potential classpath
   */
  @Internal
  public PotentialMinecraftClasspath getPotentialClasspath() {
    return potentialClasspath;
  }

  /**
   * Sets the potential classpath. Shortly before executing the matching source sets will be filtered, so that source
   * sets which don't match the minecraft version don't end up on the classpath.
   *
   * @param potentialClasspath The potential classpath
   */
  public void setPotentialClasspath(PotentialMinecraftClasspath potentialClasspath) {
    this.potentialClasspath = potentialClasspath;
  }

  /**
   * Sets the asset index minecraft uses.
   *
   * @param assetIndex The asset index
   */
  public void setAssetIndex(String assetIndex) {
    this.assetIndex = assetIndex;
  }

  /**
   * Sets the path to the assets store.
   *
   * @param assetsPath The path to the assets store
   */
  public void setAssetsPath(Path assetsPath) {
    this.assetsPath = assetsPath;
  }

  /**
   * Sets the path where minecraft should store its natives.
   *
   * @param nativesDirectory The directory where minecraft should store its natives
   */
  public void setNativesDirectory(Path nativesDirectory) {
    this.nativesDirectory = nativesDirectory;
  }

  /**
   * Sets the authenticator that will be used for logging in into minecraft if the user desires so.
   *
   * @param authenticator The authenticator used for logging in into minecraft
   */
  public void setAuthenticator(YggdrasilAuthenticator authenticator) {
    this.authenticator = authenticator;
  }

  /**
   * Launches minecraft with the current configuration.
   *
   * @throws IllegalStateException If a property has not been configured
   */
  @TaskAction
  @Override
  public void exec() {
    if(configurationName == null) {
      throw new IllegalStateException("The configuration name has not been configured");
    } else if(version == null) {
      throw new IllegalStateException("The version has not been configured");
    } else if(versionedArguments == null) {
      throw new IllegalStateException("Versioned arguments have not been configured");
    } else if(potentialClasspath == null) {
      throw new IllegalStateException("The potential classpath has not been configured");
    } else if(assetIndex == null) {
      throw new IllegalStateException("The asset index has not been configured");
    } else if(assetsPath == null) {
      throw new IllegalStateException("The assets path has not been configured");
    } else if(nativesDirectory == null) {
      throw new IllegalStateException("The natives directory has not been configured");
    }

    // Configure variables
    Map<String, String> variables = new HashMap<>(DEFAULT_VARIABLES);
    variables.put("version_name", version + "(Flint-Gradle)");
    variables.put("game_directory", getWorkingDir().getAbsolutePath());
    variables.put("assets_index_name", assetIndex);
    variables.put("assets_root", assetsPath.toString());
    variables.put("version_type", versionType);
    variables.put("natives_directory", nativesDirectory.toString());

    // Set up the login
    if(!setup(variables)) {
      // User cancelled run
      return;
    }

    System.out.println(versionedArguments.getJvm());
    System.out.println(versionedArguments.getGame());

    // Resolve the commandline arguments
    List<String> jvmArgs = resolveArguments(versionedArguments.getJvm(), variables, true);
    List<String> programArgs = resolveArguments(versionedArguments.getGame(), variables, false);

    // Set the JVM arguments
    jvmArgs(jvmArgs);

    // Create the config file
    Path log4jConfiguration = generateLog4jConfigFile();
    jvmArgs("-Dlog4j.configurationFile=" + log4jConfiguration.toString());
    args(programArgs);
    args("--game-version", version);

    // Add the classpath
    classpath(potentialClasspath.getRealClasspath(getProject(), version));

    try {
      super.exec();
    } finally {
      // TODO: This would delete a user supplied file!
      /* try {
        // Delete the temporary configuration file, it will be regenerated on next run
        Files.delete(log4jConfiguration);
      } catch(IOException e) {
        getLogger().warn("Failed to delete temporary log4j configuration", e);
      } */
    }
  }

  /**
   * Resolves the given list of minecraft launch arguments.
   *
   * @param arguments          The arguments to resolve
   * @param variables          The variables to use while resolving
   * @param stripClasspathArgs If arguments related to the java classpath should be removed while resolving
   * @return The resolved arguments
   * @throws IllegalArgumentException If an unknown variable is found
   */
  private List<String> resolveArguments(
      List<ArgumentString> arguments, Map<String, String> variables, boolean stripClasspathArgs) {
    List<String> output = new ArrayList<>();

    // Iterate all arguments
    for(ArgumentString argument : arguments) {
      // Test if the arguments apply to this platform, if not, skip them
      if(!RuleChainResolver.testRuleChain(argument.getRules())) {
        continue;
      }

      // Extract the value of the argument
      String value = argument.getValue();
      if(stripClasspathArgs && value.equals("-cp")) {
        // Found -cp and strip classpath is enabled, skip the argument
        continue;
      }

      // Extract the variable name
      String variableName = argument.getVariableName();
      if(variableName != null) {
        // Found a variable
        if(stripClasspathArgs && variableName.equals("classpath")) {
          // Found the classpath variable and strip classpath is enabled, skip the argument
          continue;
        } else if(!variables.containsKey(variableName)) {
          throw new IllegalArgumentException("Unknown launch variable name " + variableName);
        }

        // Append the value of the variable to the value of the argument
        value += variables.get(variableName);
      }

      // Add the final value
      output.add(value);
    }

    return output;
  }

  /**
   * Generates the log4j config file for the current run.
   *
   * @return The path to the generated file
   */
  private Path generateLog4jConfigFile() {
    File logOverrideFile = getProject().file(".flint/log4j-override.xml");
    if(logOverrideFile.exists()) {
      // Log override exists
      return logOverrideFile.toPath();
    }

    Path outputPath;
    try {
      // Create a temporary file
      outputPath = Files.createTempFile("log4j", ".xml");

      try(
          OutputStream out = Files.newOutputStream(outputPath);
          InputStream in = getClass().getResourceAsStream("/default_log4j.xml")
      ) {
        Util.copyStream(in, out);
      }
    } catch(IOException e) {
      throw new FlintGradleException("IO error occurred while creating logging configuration", e);
    }

    return outputPath;
  }

  /**
   * Opens an {@link LoginDialog} to the user if the project has set the required properties and fills the variables for
   * launch.
   *
   * @param variables The variables to fill for launching
   * @return {@code true} if the launch should continue, {@code false} otherwise
   */
  private boolean setup(Map<String, String> variables) {
    if(!FlintPluginProperties.ENABLE_MINECRAFT_LOGIN.resolve(getProject())) {
      // Continue launching offline, the user has not requested online launching
      return true;
    }

    if(authenticator == null) {
      getLogger().warn("Can't login because gradle is operating in offline mode!");
      return true;
    }

    String errorMessage = null;
    try {
      if(!authenticator.requiresReAuth()) {
        // There are still valid login details cached, use them
        getLogger().lifecycle("Using cached minecraft login token");
        setAuthVariables(variables);
        return true;
      } else if(authenticator.refreshToken()) {
        // The token has been refreshed successfully
        getLogger().lifecycle("Using refreshed minecraft login token");
        setAuthVariables(variables);
        return true;
      }
    } catch(IOException e) {
      getLogger().error("IOException while trying to re-auth, asking auth again", e);
      errorMessage = e.getMessage();
    }

    String email = null;

    while(true) {
      // Open the login dialog
      LoginDialog dialog = new LoginDialog(email, errorMessage);
      LoginDialogResult result = dialog.execute();

      if(result == null) {
        // User closed window
        return false;
      }

      // Handle the result accordingly
      switch(result) {
        case ABORT:
          // User aborted launch
          return false;

        case ATTEMPT_LOGIN: {
          // User filled in login form
          email = dialog.getEmail();
          String password = dialog.getPassword();

          try {
            authenticator.authenticate(email, password);
            setAuthVariables(variables);  // Authentication succeeded
            return true;
          } catch(YggdrasilAuthenticationException e) {
            getLogger().error("Failed to log in into minecraft", e);
            errorMessage = e.getMessage();
          } catch(IOException e) {
            // This should never happen, unless some AntiVirus or something
            // like that decides to deny access to cached files
            throw new UncheckedIOException("Failed to read login cache", e);
          }
          break;
        }

        case CONTINUE_OFFLINE:
          return true;
      }

    }
  }

  /**
   * Reads the cached login data from the authenticator and applies it to the given map.
   *
   * @param variables The map to put the login data into
   * @throws IOException If an I/O error occurs while reading the cached variables
   */
  private void setAuthVariables(Map<String, String> variables) throws IOException {
    variables.put("auth_player_name", authenticator.getCachedPlayerName());
    variables.put("auth_uuid", authenticator.getCachedUUID().toString());
    variables.put("auth_access_token", authenticator.getCachedToken());
  }
}
