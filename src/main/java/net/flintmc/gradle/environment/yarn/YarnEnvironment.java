package net.flintmc.gradle.environment.yarn;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import net.flintmc.gradle.environment.DefaultDeobfuscationEnvironment;
import net.flintmc.gradle.environment.DeobfuscationException;
import net.flintmc.gradle.environment.DeobfuscationUtilities;
import net.flintmc.gradle.environment.EnvironmentCacheFileProvider;
import net.flintmc.gradle.environment.SourceJarProcessor;
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

public class YarnEnvironment extends DefaultDeobfuscationEnvironment {
  private static final Logger LOGGER = Logging.getLogger(YarnEnvironment.class);

  private final DefaultInput input;

  public YarnEnvironment(DefaultInput input) {
    super(input, "yarn", EnvironmentType.YARN);
    this.input = input;
  }

  /** {@inheritDoc} */
  @Override
  public void runDeobfuscation(
      MavenPom clientPom, MavenPom serverPom, DeobfuscationUtilities utilities)
      throws DeobfuscationException {

    EnvironmentCacheFileProvider cacheFileProvider = utilities.getCacheFileProvider();
    OkHttpClient httpClient = utilities.getHttpClient();

    Path yarnConfigurationOutput =
        this.downloadAndExtractZip(
            LOGGER,
            cacheFileProvider,
            httpClient,
            this.input.getConfigDownload().toExternalForm(),
            "yarn-config_" + this.input.getMappingsVersion());

    Path yarnMappingsOutput =
        this.downloadAndExtractZip(
            LOGGER,
            cacheFileProvider,
            httpClient,
            this.input.getMappingsDownload().toExternalForm(),
            "yarn-mappings_" + this.input.getMappingsVersion());

    YarnRun run = new YarnRun(clientPom, serverPom, utilities, yarnConfigurationOutput);

    try {
      run.loadData();
    } catch (IOException exception) {
      throw new DeobfuscationException("Failed to load data for yarn run", exception);
    }

    List<String> sides = new CopyOnWriteArrayList<>();

    if (clientPom != null) {
      sides.add("client");
    }

    if (serverPom != null) {
      sides.add("server");
    }

    String version = getVersion(clientPom, serverPom);

    List<Path> clientLibraries = null;

    if (clientPom != null) {

      SimpleMavenRepository internalRepository = utilities.getInternalRepository();

      clientLibraries = new CopyOnWriteArrayList<>();

      for (MavenDependency dependency : clientPom.getDependencies()) {
        if (!internalRepository.isInstalled(dependency)) {
          throw new IllegalStateException(
              String.format("The minecraft dependency %s is not installed!", dependency));
        }

        clientLibraries.add(internalRepository.getArtifactPath(dependency));
      }
    }

    for (String side : sides) {
      run.prepare(side);
    }

    Map<String, Path> outputs = new HashMap<>();

    for (String side : sides) {

      outputs.put(side, run.execute(side));
    }

    SourceJarProcessor processor = new SourceJarProcessor();
    processor.addAction(source -> {});

    MinecraftRepository minecraftRepository = utilities.getMinecraftRepository();

    for (Entry<String, Path> entry : outputs.entrySet()) {

      String side = entry.getKey();
      Path path = entry.getValue();

      MavenArtifact sourceArtifact =
          new MavenArtifact("net.minecraft", side, version, this.getClassifier(true));

      Path sourcesTargetArtifactPath = minecraftRepository.getArtifactPath(sourceArtifact);

      if (!Files.isDirectory(sourcesTargetArtifactPath.getParent())) {
        try {
          Files.createDirectories(sourcesTargetArtifactPath.getParent());
        } catch (IOException exception) {
          throw new DeobfuscationException(
              "Failed to create parent directory for target artifact", exception);
        }
      }

      try {

        LOGGER.lifecycle("Processing yarn to deobfuscated for {} {}", side, version);
        LOGGER.lifecycle("Input: " + path.toAbsolutePath().toString());
        LOGGER.lifecycle("Output: " + sourcesTargetArtifactPath.toAbsolutePath().toString());
        processor.process(path, sourcesTargetArtifactPath);
      } catch (IOException exception) {
        throw new DeobfuscationException(
            String.format("Failed to process %s %s", side, version), exception);
      }

      if (clientLibraries != null) {

        Path sourceDir = null;

        MavenArtifact outputArtifact =
            new MavenArtifact("net.minecraft", side, version, this.getClassifier(false));

        Path outputPath = minecraftRepository.getArtifactPath(outputArtifact);

        try {

          sourceDir = Util.temporaryDir();
          Util.extractZip(sourcesTargetArtifactPath, sourceDir);
          /*
                    if (side.equals("client")) {

                      JavaExecutionResult compilationResult =
                          utilities.getJavaCompileHelper().compile(sourceDir, clientLibraries, outputPath);

                      if (compilationResult.getExitCode() != 0) {
                        LOGGER.error("Minecraft {} {} failed to recompile", side, version);
                        LOGGER.error("javac output:");
                        LOGGER.error(compilationResult.getStdout());
                        LOGGER.error("javac error:");
                        LOGGER.error(compilationResult.getStderr());
                        throw new DeobfuscationException(
                            String.format("Failed to recompile %s %s", side, version));
                      } else {
                        LOGGER.lifecycle("Done!");
                      }
                    }
          */
          Path pomPath = minecraftRepository.getPomPath(outputArtifact);

          if (!Files.exists(pomPath)) {
            MavenPom pom = new MavenPom(outputArtifact);
            pom.addDependencies(clientPom.getDependencies());
            minecraftRepository.addPom(pom);
          }
        } catch (IOException exception) {
          throw new DeobfuscationException(
              "IO exception while deobfuscating " + side + " " + version, exception);
        } finally {
          if (sourceDir != null) {

            try {
              Util.nukeDirectory(sourceDir, true);
            } catch (IOException exception) {
              LOGGER.warn("Failed to delete temporary directory {}", sourceDir.toString());
            }
          }
        }
      }
    }
  }

  @Override
  public Collection<MavenArtifact> getCompileArtifacts(MavenArtifact client, MavenArtifact server) {
    return Collections.singleton(this.getClientArtifact(this.getVersion(client, server)));
  }

  @Override
  public Collection<MavenArtifact> getRuntimeArtifacts(MavenArtifact client, MavenArtifact server) {
    return Collections.singleton(this.getClientArtifact(this.getVersion(client, server)));
  }
}
