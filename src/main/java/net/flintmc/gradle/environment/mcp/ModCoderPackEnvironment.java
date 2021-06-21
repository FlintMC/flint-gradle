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

package net.flintmc.gradle.environment.mcp;

import com.google.common.collect.ImmutableMap;
import net.flintmc.gradle.environment.*;
import net.flintmc.gradle.java.exec.JavaExecutionResult;
import net.flintmc.gradle.maven.SimpleMavenRepository;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.maven.pom.MavenDependency;
import net.flintmc.gradle.maven.pom.MavenPom;
import net.flintmc.gradle.minecraft.MinecraftRepository;
import net.flintmc.gradle.minecraft.data.environment.DefaultInput;
import net.flintmc.gradle.minecraft.data.environment.EnvironmentType;
import net.flintmc.gradle.util.Util;
import okhttp3.OkHttpClient;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the MCP as a deobfuscation environment.
 */
public class ModCoderPackEnvironment extends DefaultDeobfuscationEnvironment {
  private static final Logger LOGGER = Logging.getLogger(ModCoderPackEnvironment.class);

  private final DefaultInput input;

  /**
   * Constructs a new MCP deobfuscation environment with the given input data.
   *
   * @param input The data to obtain MCP files from
   */
  public ModCoderPackEnvironment(DefaultInput input) {
    super(input, "mcp", EnvironmentType.MOD_CODER_PACK);
    this.input = input;
  }

  @Override
  public Map<String, File> getDownloadedMappingFiles(OkHttpClient httpClient, EnvironmentCacheFileProvider cacheFileProvider) throws DeobfuscationException {

    return ImmutableMap.of(
        "mcp-config",
        this.downloadAndExtractZip(
            LOGGER,
            cacheFileProvider,
            httpClient,
            input.getConfigDownload().toExternalForm(),
            "mcp-config_" + input.getConfigVersion()).toFile(),
        "mcp-mappings",
        this.downloadAndExtractZip(
                LOGGER,
                cacheFileProvider,
                httpClient,
                input.getMappingsDownload().toExternalForm(),
                "mcp-mappings_" + input.getMappingsVersion())
            .toFile()
    );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void runDeobfuscation(
      MavenPom clientPom, MavenPom serverPom, DeobfuscationUtilities utilities)
      throws DeobfuscationException {
    // Get a few utilities classes out of the instance variable
    EnvironmentCacheFileProvider cacheFileProvider = utilities.getCacheFileProvider();
    OkHttpClient httpClient = utilities.getHttpClient();

    // Download and extract the inputs if required
    Path mcpConfigOutput =
        this.downloadAndExtractZip(
            LOGGER,
            cacheFileProvider,
            httpClient,
            input.getConfigDownload().toExternalForm(),
            "mcp-config_" + input.getConfigVersion());
    Path mappingsOutput =
        this.downloadAndExtractZip(
            LOGGER,
            cacheFileProvider,
            httpClient,
            input.getMappingsDownload().toExternalForm(),
            "mcp-mappings_" + input.getMappingsVersion());

    // Create the runner
    ModCoderPackRun run = new ModCoderPackRun(clientPom, serverPom, utilities, mcpConfigOutput);

    try {
      run.loadData();
    } catch (IOException e) {
      throw new DeobfuscationException("Failed to load data for MCP run", e);
    }

    // Save all sides which should be executed
    List<String> sides = new ArrayList<>();

    if (clientPom != null) {
      // If the client exists, deobfuscate it
      sides.add("client");
    }

    if (serverPom != null) {
      // If the server exists, deobfuscate it
      sides.add("server");
    }

    if (clientPom != null && serverPom != null) {
      // If client and server exist, create the joined/merged jars too
      sides.add("joined");
    }

    // Try to retrieve the version
    String version = getVersion(clientPom, serverPom);

    List<Path> clientLibraries = null;
    if (clientPom != null) {
      SimpleMavenRepository internalRepository = utilities.getInternalRepository();

      // Collect the client libraries if the client POM is available
      clientLibraries = new ArrayList<>();

      for (MavenDependency dependency : clientPom.getDependencies()) {
        if (!internalRepository.isInstalled(dependency)) {
          throw new IllegalStateException(
              "The minecraft dependency " + dependency + " is not installed");
        }

        // Add the path of the artifact to the libraries
        clientLibraries.add(internalRepository.getArtifactPath(dependency));
      }
    }

    for (String side : sides) {
      // Prepare all sides
      run.prepare(side);
    }

    Map<String, Path> outputs = new HashMap<>();

    for (String side : sides) {
      // Execute all sides
      outputs.put(side, run.execute(side));
    }

    // Construct the source jar processor
    SourceJarProcessor processor = new SourceJarProcessor();

    // Construct the CSV remapper
    CsvRemapper remapper = new CsvRemapper();
    try {
      Files.walk(mappingsOutput)
          .filter(Files::isRegularFile)
          .filter((path) -> path.getFileName().toString().endsWith(".csv"))
          .forEach(
              (path) -> {
                try {
                  // Load all CSV files as mappings
                  remapper.loadCsv(path);
                } catch (IOException e) {
                  // Will be caught by the block later down, we need to rethrow as unchecked
                  // as the lambda can't throw
                  throw new UncheckedIOException(e);
                }
              });
    } catch (UncheckedIOException | IOException e) {
      throw new DeobfuscationException("Failed to initialize remapper", e);
    }

    // Index the actions
    processor.addAction(remapper);
    processor.addAction(new ForgeAdditionStripper());

    // Retrieve utility classes
    MinecraftRepository minecraftRepository = utilities.getMinecraftRepository();

    for (Map.Entry<String, Path> output : outputs.entrySet()) {
      // Extract every side and the associated jar
      String side = output.getKey();
      Path srgArtifactPath = output.getValue();

      // Generate output artifact
      MavenArtifact sourcesArtifact =
          new MavenArtifact("net.minecraft", side, version, getClassifier(true));

      // Retrieve the path the artifact should be written to
      Path sourcesTargetArtifactPath = minecraftRepository.getArtifactPath(sourcesArtifact);
      if (!Files.isDirectory(sourcesTargetArtifactPath.getParent())) {
        // Make sure we can write the artifact
        try {
          Files.createDirectories(sourcesTargetArtifactPath.getParent());
        } catch (IOException e) {
          throw new DeobfuscationException("Failed to create parent directory for target artifact", e);
        }
      }

      try {
        // Remap the SRG source jar to deobfuscated jar
        LOGGER.lifecycle("Processing SRG to deobfuscated for {} {}", side, version);
        processor.process(srgArtifactPath, sourcesTargetArtifactPath);
      } catch (IOException e) {
        throw new DeobfuscationException("Failed to process " + side + " " + version, e);
      }

      if (clientLibraries != null) {
        // Recompilation can only be done if the client libraries are known
        Path sourceDir = null;

        // Generate output artifact
        MavenArtifact outputArtifact =
            new MavenArtifact("net.minecraft", side, version, getClassifier(false));

        // Get the path of the output artifact
        Path outputPath = minecraftRepository.getArtifactPath(outputArtifact);

        try {
          // Extract the sources jar for recompilation
          sourceDir = Util.temporaryDir();
          Util.extractZip(sourcesTargetArtifactPath, sourceDir);

          LOGGER.lifecycle("Recompiling {} {}", side, version);

          // Set up the compilation
          JavaExecutionResult compilationResult =
              utilities.getJavaCompileHelper().compile(sourceDir, clientLibraries, outputPath);

          if (compilationResult.getExitCode() != 0) {
            // Compilation failed, bail out
            LOGGER.error("Minecraft {} {} failed to recompile", side, version);
            LOGGER.error("javac output:");
            LOGGER.error(compilationResult.getStdout());
            LOGGER.error("javac error:");
            LOGGER.error(compilationResult.getStderr());
            throw new DeobfuscationException("Failed to recompile " + side + " " + version);
          } else {
            LOGGER.lifecycle("Done!");
          }

          Path pomPath = minecraftRepository.getPomPath(outputArtifact);
          if (!Files.exists(pomPath)) {
            // There is no POM for the given artifact, generate one
            MavenPom pom = new MavenPom(outputArtifact);
            pom.addDependencies(clientPom.getDependencies());
            minecraftRepository.addPom(pom);
          }

          if (side.equals("joined")) {
            LOGGER.lifecycle("Copying over resources to joined jar");

            // Copy over the resources to the joined jar
            addResources(minecraftRepository.getArtifactPath(clientPom), outputPath);

            if (serverPom != null) {
              // Also copy server resources
              addResources(minecraftRepository.getArtifactPath(serverPom), outputPath);
            }
          }
        } catch (IOException e) {
          throw new DeobfuscationException("IO exception while deobfuscating " + side + " " + version, e);
        } finally {
          if (sourceDir != null) {
            try {
              // Try to delete the temporary source directory
              Util.nukeDirectory(sourceDir, true);
            } catch (IOException e) {
              LOGGER.warn("Failed to delete temporary directory {}", sourceDir.toString());
            }
          }
        }
      } else {
        LOGGER.warn("Can't recompile {} {}, missing client libraries", side, version);
      }
    }
  }

}
