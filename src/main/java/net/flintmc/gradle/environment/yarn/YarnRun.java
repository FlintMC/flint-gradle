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

package net.flintmc.gradle.environment.yarn;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.flintmc.gradle.environment.DeobfuscationException;
import net.flintmc.gradle.environment.DeobfuscationUtilities;
import net.flintmc.gradle.environment.EnvironmentRunnable;
import net.flintmc.gradle.environment.function.Function;
import net.flintmc.gradle.environment.function.InjectFunction;
import net.flintmc.gradle.environment.function.JavaExecutionFunction;
import net.flintmc.gradle.environment.function.JavaExecutionTemplate;
import net.flintmc.gradle.environment.function.ListLibrariesFunction;
import net.flintmc.gradle.environment.function.PatchFunction;
import net.flintmc.gradle.environment.function.StripFunction;
import net.flintmc.gradle.environment.yarn.function.YarnPatchFunction;
import net.flintmc.gradle.extension.FlintPatcherExtension;
import net.flintmc.gradle.json.JsonConverter;
import net.flintmc.gradle.maven.RemoteMavenRepository;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.maven.pom.MavenPom;
import net.flintmc.gradle.minecraft.MinecraftRepository;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

public class YarnRun implements EnvironmentRunnable {

  private static final Logger LOGGER = Logging.getLogger("Yarn");

  private final Map<String, Path> variables;
  private final Map<String, JavaExecutionTemplate> javaFunctions;
  private final Map<String, List<Function>> steps;

  private final MavenPom clientJar;
  private final MavenPom serverJar;
  private final DeobfuscationUtilities utilities;
  private final Path yarnPath;
  private final Path stepsPath;

  public YarnRun(
      MavenPom clientJar, MavenPom serverJar, DeobfuscationUtilities utilities, Path yarnPath) {
    this.variables = new HashMap<>();
    this.javaFunctions = new HashMap<>();
    this.steps = new HashMap<>();

    this.clientJar = clientJar;
    this.serverJar = serverJar;
    this.utilities = utilities;
    this.yarnPath = yarnPath;
    this.stepsPath = yarnPath.resolve("steps");
  }

  /**
   * Extracts the given zip file to the given directory.
   *
   * @param zip The path to the zip file to extract
   * @param targetDir The directory to extract the zip file into
   * @param options Options to pass to {@link Files#copy(InputStream, Path, CopyOption...)}
   * @throws IOException If an I/O error occurs while reading or writing files
   */
  public void extractZip(Path zip, Path targetDir, CopyOption... options) throws IOException {
    try (ZipFile zipFile = new ZipFile(zip.toFile())) {
      // Get a list of all entries
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.isDirectory()) {
          // Required directories will be created automatically
          continue;
        }

        String name = entry.getName();

        if (name.contains("assets")
            || name.contains("pack.png")
            || name.contains("META-INF")
            || name.contains("log4j2.xml")) {
          continue;
        }

        if (name.startsWith("/")) {
          // Make sure that the entry does not start with a /, else it will corrupt
          // the Path#resolve result
          name = name.substring(1);
        }

        Path targetFile = targetDir.resolve(name);
        if (!Files.exists(targetFile.getParent())) {
          // Make sure the parent directories exist
          Files.createDirectories(targetFile.getParent());
        }

        try (InputStream entryStream = zipFile.getInputStream(entry)) {
          // Copy the entire entry to the target file

          if (!targetFile.toFile().exists()) Files.copy(entryStream, targetFile, options);
        }
      }
    }
  }

  @Override
  public void loadData() throws IOException {
    Path configJson = yarnPath.resolve("config.json");

    if (!Files.isRegularFile(configJson)) {
      throw new FileNotFoundException("config.json not found: does not exist or is not a file!");
    }

    JsonNode configRoot;

    try (InputStream inputStream = Files.newInputStream(configJson)) {
      configRoot = JsonConverter.OBJECT_MAPPER.readTree(inputStream);
    }

    JsonNode dataNode = configRoot.get("data").requireNonNull();

    int specVersion = configRoot.get("spec").asInt();

    if (specVersion != 1) {
      throw new UnsupportedOperationException(
          "The yarn environment only supports spec version 1, but got " + specVersion);
    }

    for (Iterator<String> iterator = dataNode.fieldNames(); iterator.hasNext(); ) {

      String variableName = iterator.next();
      JsonNode variableNode = dataNode.get(variableName);

      switch (variableNode.getNodeType()) {
        case STRING:
          {
            this.variables.put(variableName, this.resolveConfigPath(variableNode.asText()));
            break;
          }

        case OBJECT:
          {
            for (Iterator<String> nestedIterator = variableNode.fieldNames();
                nestedIterator.hasNext(); ) {

              String nestedName = nestedIterator.next();
              JsonNode nestedNode = variableNode.get(nestedName);

              variables.put(
                  nestedName + "|" + variableName, this.resolveConfigPath(nestedNode.asText()));
            }

            break;
          }

        default:
          throw new UnsupportedOperationException(
              String.format(
                  "Found data type %s on yarn input data, expected STRING or OBJECT!",
                  variableNode.getNodeType().name()));
      }
    }

    MinecraftRepository minecraftRepository = utilities.getMinecraftRepository();

    if (this.clientJar != null) {
      this.variables.put(
          "downloadClientOutput", minecraftRepository.getArtifactPath(this.clientJar));
    }

    if (this.serverJar != null) {
      this.variables.put(
          "downloadServerOutput", minecraftRepository.getArtifactPath(this.serverJar));
    }

    this.processJavaFunctions(configRoot.get("functions").requireNonNull());

    JsonNode stepsNode = configRoot.get("steps").requireNonNull();

    for (Iterator<String> iterator = stepsNode.fieldNames(); iterator.hasNext(); ) {

      String sideName = iterator.next();

      Path sidedLog = this.stepsPath.resolve(sideName).resolve("other.log");

      if (Files.isDirectory(sidedLog.getParent())) {
        Files.createDirectories(sidedLog.getParent());
      }

      this.variables.put(sideName + "|log", sidedLog);

      this.processSteps(stepsNode.get(sideName), sideName);
    }
  }

  private void processSteps(JsonNode steps, String sideName) {

    if (!steps.isArray()) {
      throw new IllegalArgumentException(
          String.format("Expected steps of %s to be an array!", sideName));
    }

    List<Function> sidedSteps =
        this.steps.compute(
            sideName,
            (k, v) -> {
              if (v == null) {
                return new ArrayList<>();
              } else {
                throw new IllegalStateException(
                    String.format("Steps for side %s processed already", sideName));
              }
            });

    for (int i = 0; i < steps.size(); i++) {
      JsonNode stepNode = steps.path(i);

      Map<String, String> values = new HashMap<>();

      for (Iterator<String> iterator = stepNode.fieldNames(); iterator.hasNext(); ) {
        String key = iterator.next();
        values.put(key, stepNode.get(key).requireNonNull().asText());
      }

      String type = values.get("type");

      if (type == null) {
        throw new IllegalArgumentException("Missing type value for step!");
      } else if (shouldIgnoreStep(type)) {
        continue;
      }

      String name = values.getOrDefault("name", type);
      String input =
          values.containsKey("input")
              ? resolveVariableValue(values.get("input"), sideName, values)
              : null;

      if (input != null) {
        values.put("input", input);
      }

      if (this.javaFunctions.containsKey(type)) {
        Path output = createOutput(sideName, name, "jar");
        values.put("output", output.toString());

        JavaExecutionTemplate template = this.javaFunctions.get(type);

        List<String> arguments = new ArrayList<>();
        for (String arg : template.getArgs()) {
          arguments.add(resolveVariableValue(arg, sideName, values));
        }

        JavaExecutionFunction function =
            new JavaExecutionFunction(
                name,
                output,
                template.getExecutionArtifact(),
                utilities.getHttpClient() == null
                    ? null
                    : new RemoteMavenRepository(
                        utilities.getHttpClient(), template.getExecutionArtifactRepo()),
                arguments,
                template.getJvmArgs());

        sidedSteps.add(function);
        continue;
      }

      switch (type) {
        case "inject":
          {
            if (input == null) {
              throw new IllegalArgumentException("The inject function always requires an input");
            }

            Path output = createOutput(sideName, name, "jar");
            values.put("output", output.toString());

            // Construct the inject function
            InjectFunction function = new InjectFunction(name, output, Paths.get(input), "yarn");
            sidedSteps.add(function);
            break;
          }

        case "strip":
          {
            if (input == null) {
              throw new IllegalArgumentException("The strip function always requires an input");
            }

            if (!variables.containsKey("mappings")) {
              // This will hopefully never happen
              throw new IllegalArgumentException(
                  "The strip functions requires mappings to be supplied");
            }

            Path output = createOutput(sideName, name, "jar");
            values.put("output", output.toString());

            // Construct the strip function
            StripFunction function =
                new StripFunction(
                    name,
                    variables.get("mappings"),
                    Paths.get(input),
                    output,
                    // It is not clear if this value ever appears, but the MCP checks for it, so do
                    // we
                    // By default the mode is always whitelist
                    resolveVariableValue(values.getOrDefault("mode", "whitelist"), sideName, values)
                        .equalsIgnoreCase("whitelist"));

            sidedSteps.add(function);
            break;
          }

        case "listLibraries":
          {
            Path output = createOutput(sideName, name, "txt");
            values.put("output", output.toString());

            // Construct the listLibraries function
            ListLibrariesFunction function = new ListLibrariesFunction(name, output, clientJar);

            sidedSteps.add(function);
            break;
          }

        case "patch":
        {
          if (input == null) {
            throw new IllegalArgumentException("The patch function always requires an input");
          }

          Path output = createOutput(sideName, name, "jar");
          values.put("output", output.toString());

          // Resolve the patches input
          Path patches = Paths.get(resolveVariableValue("{patches}", sideName, values));

          // Construct the patch function
          PatchFunction function = new YarnPatchFunction(name, Paths.get(input), output, patches);

          sidedSteps.add(function);
          break;
        }


        default:
          {
            throw new IllegalArgumentException(
                "Got task "
                    + name
                    + " of type "
                    + type
                    + " which is neither "
                    + "a builtin function or defined via the java functions");
          }
      }
    }
  }

  /**
   * Creates the output path with the associated variable for the given step.
   *
   * @param stepName The name of the step to create the output for
   * @param side The side the output belongs to
   * @param fileExtension The file extension of the output
   * @return The path to the created output path
   */
  private Path createOutput(String side, String stepName, String fileExtension) {
    Path path = stepsPath.resolve(side).resolve(stepName + '.' + fileExtension);
    variables.put(side + "|" + stepName + "Output", path);
    return path;
  }

  /**
   * Recursively resolves the value of the given variable name.
   *
   * @param input The variable name or value of the variable itself. Will be treated as a variable
   *     if starts with <code>{</code> and ends with <code>}</code>.
   * @param side The side the variable is being resolved for
   * @param extraVariables Extra variables to resolves values from
   * @return The resolved variable, or {@code null}, if the input name was null
   * @throws IllegalArgumentException If the input is a variable but no does not exist
   */
  private String resolveVariableValue(
      String input, String side, Map<String, String> extraVariables) {
    if (input == null) {
      // Map null to null
      return null;
    }

    String previous;
    String value = input;
    do {
      // Keep resolving until the variable does not change anymore
      previous = value;
      value = resolveOne(value, side, extraVariables);
    } while (!value.equals(previous));

    return value;
  }

  /**
   * Resolves the value of the given variable name.
   *
   * @param input The variable name or value of the variable itself. Will be treated as a variable
   *     if starts with <code>{</code> and ends with <code>}</code>.
   * @param side The side the variable is being resolved for
   * @param extraVariables Extra variables to resolves values from
   * @return The resolved variable, or {@code null}, if the input name was null
   * @throws IllegalArgumentException If the input is a variable but no does not exist
   */
  private String resolveOne(String input, String side, Map<String, String> extraVariables) {
    if (!input.startsWith("{") || !input.endsWith("}")) {
      // The input is not a variable, but a value
      return input;
    }

    // Remove the { and } from the name
    input = input.substring(1, input.length() - 1);
    if (extraVariables != null && extraVariables.containsKey(input)) {
      // The extra variables contain the given variable, take it from there
      return extraVariables.get(input);
    }

    if (!variables.containsKey(input)) {
      // Construct the sided name of the variable
      String sidedName = side + "|" + input;
      if (!variables.containsKey(sidedName)) {
        // The variable was not found in extra nor global variables nor as a sided variable
        throw new IllegalArgumentException(
            "Variable " + input + " (sided " + sidedName + ") does not exist");
      }

      // The variable is a sided one
      return variables.get(sidedName).toString();
    }

    return variables.get(input).toString();
  }

  private boolean shouldIgnoreStep(String stepNode) {
    switch (stepNode) {
        // Internal steps, those are done in advance and don't have to
        // be controlled by the MCP environment
      case "downloadManifest":
      case "downloadJson":
      case "downloadServer":
      case "downloadClient":
        return true;

        // Other steps, need to be executed
      default:
        return false;
    }
  }

  private void processJavaFunctions(JsonNode functions) throws IOException {

    for (Iterator<String> iterator = functions.fieldNames(); iterator.hasNext(); ) {

      String functionName = iterator.next();
      JsonNode functionNode = functions.get(functionName).requireNonNull();

      String repo = functionNode.get("repo").requireNonNull().asText();
      MavenArtifact artifact =
          new MavenArtifact(functionNode.get("version").requireNonNull().asText());

      List<String> arguments = new CopyOnWriteArrayList<>();
      JsonNode argsNode = functionNode.get("args");

      for (int i = 0; i < argsNode.size(); i++) {
        arguments.add(argsNode.path(i).requireNonNull().asText());
      }

      List<String> jvmArguments = new CopyOnWriteArrayList<>();
      if (functionNode.has("jvmargs")) {
        JsonNode jvmArgumentsNode = functionNode.get("jvmargs");

        for (int i = 0; i < jvmArgumentsNode.size(); i++) {
          jvmArguments.add(jvmArgumentsNode.path(i).requireNonNull().asText());
        }
      }

      URI repoUri;

      try {
        repoUri = new URI(repo);
      } catch (URISyntaxException exception) {
        throw new IOException("Failed to parse " + repo + " as a URI!", exception);
      }

      this.javaFunctions.put(
          functionName, new JavaExecutionTemplate(repoUri, artifact, arguments, jvmArguments));
    }
  }

  @Override
  public void prepare(String side) throws DeobfuscationException {
    if (!this.steps.containsKey(side)) {
      throw new IllegalArgumentException("No steps defined for side " + side);
    }

    List<Function> functions = this.steps.get(side);

    for (Function function : functions) {
      Path output = function.getOutput();

      if (!Files.isRegularFile(output)) {
        Path outputDir = output.getParent();

        try {
          Files.createDirectories(outputDir);
        } catch (IOException exception) {
          throw new DeobfuscationException(
              String.format(
                  "Failed to create output directory for step %s for %s",
                  function.getName(), side));
        }
      }

      LOGGER.lifecycle("Preparing Yarn step {} for {}", function.getName(), side);
      function.prepare(this.utilities);
    }
  }

  @Override
  public Path execute(String side) throws DeobfuscationException {
    if (!steps.containsKey(side)) {
      throw new IllegalArgumentException("No steps defined for side " + side);
    }

    List<Function> sidedSteps = steps.get(side);

    boolean forceFollowingSteps = false;

    // Iterate over every step of the given side
    for (int i = 0; i < sidedSteps.size(); i++) {
      Function step = sidedSteps.get(i);
      Path output = step.getOutput();

      if (forceFollowingSteps && Files.exists(output)) {
        // If the previous step has been run and the output for the current step exists, delete it
        try {
          Files.delete(output);
        } catch (IOException e) {
          throw new DeobfuscationException("Failed to delete outdated output", e);
        }
      }

      if (!Files.isRegularFile(output)) {
        // The output does not exist, execute the step
        LOGGER.lifecycle(
            "[{}/{}] Running Yarn step {} for {}", i + 1, sidedSteps.size(), step.getName(), side);
        long startMillis = System.currentTimeMillis();
        try {
          step.execute(utilities);
        } catch (DeobfuscationException | RuntimeException exception) {
          try {
            // If an exception occurred, try to delete the file
            Files.deleteIfExists(output);
          } catch (IOException innerException) {
            LOGGER.error(
                "Failed to delete output after step failed, please manually clear the cache!",
                innerException);
          }
          throw exception;
        }
        long timeTaken = System.currentTimeMillis() - startMillis;

        if (timeTaken < 1000) {
          LOGGER.lifecycle("[{}/{}] Done, took {}ms", i + 1, sidedSteps.size(), timeTaken);
        } else {
          LOGGER.lifecycle("[{}/{}] Done, took {}s", i + 1, sidedSteps.size(), timeTaken / 1000);
        }

        forceFollowingSteps = true;
      } else {
        // The output exists already
        LOGGER.lifecycle(
            "[{}/{}] Skipping Yarn step {} for {}, output exists already",
            i + 1,
            sidedSteps.size(),
            step.getName(),
            side);
      }

      if (side.equals("client") && output.getFileName().toString().contains("decompile")) {

        if (FlintPatcherExtension.HackyPatcherData.isEnabled()) {
          try {
            extractZip(output, FlintPatcherExtension.HackyPatcherData.getCleanSource().toPath());
            extractZip(output, FlintPatcherExtension.HackyPatcherData.getModifiedSource().toPath());
          } catch (IOException exception) {
            exception.printStackTrace();
          }
        }
      }
    }

    return sidedSteps.get(sidedSteps.size() - 1).getOutput();
  }

  private Path resolveConfigPath(String partial) {
    return this.yarnPath.resolve(partial);
  }
}
