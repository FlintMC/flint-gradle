package net.flintmc.gradle.environment.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import net.flintmc.gradle.environment.DeobfuscationException;
import net.flintmc.gradle.environment.DeobfuscationUtilities;
import net.flintmc.gradle.environment.mcp.function.*;
import net.flintmc.gradle.json.JsonConverter;
import net.flintmc.gradle.maven.RemoteMavenRepository;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.maven.pom.MavenPom;
import net.flintmc.gradle.minecraft.MinecraftRepository;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ModCoderPackRun {
  private static final Logger LOGGER = Logging.getLogger(ModCoderPackRun.class);

  private final Map<String, Path> variables;
  private final Map<String, JavaExecutionTemplate> javaFunctions;
  private final Map<String, List<MCPFunction>> steps;

  private final MavenPom clientJar;
  private final MavenPom serverJar;
  private final DeobfuscationUtilities utilities;
  private final Path mcpPath;
  private final Path stepsPath;

  public ModCoderPackRun(
      MavenPom clientJar, MavenPom serverJar, DeobfuscationUtilities utilities, Path mcpPath) {
    this.variables = new HashMap<>();
    this.javaFunctions = new HashMap<>();
    this.steps = new HashMap<>();

    this.clientJar = clientJar;
    this.serverJar = serverJar;
    this.utilities = utilities;
    this.mcpPath = mcpPath;
    this.stepsPath = mcpPath.resolve("steps");
  }

  public void loadData() throws IOException {
    // Get the path to the config.json
    Path configJson = mcpPath.resolve("config.json");
    if (!Files.isRegularFile(configJson)) {
      throw new FileNotFoundException("config.json not found: does not exist or is not a file");
    }

    // Read the config.json
    JsonNode configRoot;
    try (InputStream stream = Files.newInputStream(configJson)) {
      configRoot = JsonConverter.OBJECT_MAPPER.readTree(stream);
    }

    // Check that we have the correct specification version
    int specVersion = configRoot.get("spec").asInt();
    if (specVersion != 1) {
      throw new UnsupportedOperationException(
          "The MCP environment only supports spec version 1, but got " + specVersion);
    }

    // Read the input variables, all of them are paths
    JsonNode dataNode = configRoot.get("data").requireNonNull();
    for (Iterator<String> it = dataNode.fieldNames(); it.hasNext(); ) {
      // Retrieve key and value
      String variableName = it.next();
      JsonNode variableNode = dataNode.get(variableName);

      switch (variableNode.getNodeType()) {
        case STRING: {
          // Path variable without a side association
          variables.put(variableName, resolveConfigPath(variableNode.asText()));
          break;
        }

        case OBJECT: {
          // Variable is a collection of side associated paths
          for (Iterator<String> nestedIt = variableNode.fieldNames(); nestedIt.hasNext(); ) {
            // Construct the name <side>|<name>
            String nestedName = nestedIt.next();
            JsonNode nestedNode = variableNode.get(nestedName);

            // Resolve the path and save it in the variables
            variables.put(nestedName + "|" + variableName,
                resolveConfigPath(nestedNode.requireNonNull().asText()));
          }
          break;
        }

        default: {
          // Unexpected node type
          throw new UnsupportedOperationException("Found data type " + variableNode.getNodeType().name() +
              " on MCP input data, expected STRING or OBJECT");
        }
      }
    }

    MinecraftRepository minecraftRepository = utilities.getMinecraftRepository();

    // Set up own input variables
    if (clientJar != null) {
      variables.put("downloadClientOutput", minecraftRepository.getArtifactPath(clientJar));
    }

    if (serverJar != null) {
      variables.put("downloadServerOutput", minecraftRepository.getArtifactPath(serverJar));
    }

    // Process all java functions so they can be used while processing the steps
    processJavaFunctions(configRoot.get("functions").requireNonNull());

    JsonNode stepsNode = configRoot.get("steps").requireNonNull();
    for (Iterator<String> it = stepsNode.fieldNames(); it.hasNext(); ) {
      String sideName = it.next();

      // Create general log
      Path sidedLog = stepsPath.resolve(sideName).resolve("other.log");
      if (Files.isDirectory(sidedLog.getParent())) {
        Files.createDirectories(sidedLog.getParent());
      }

      variables.put(sideName + "|log", sidedLog);

      processSteps(stepsNode.get(sideName), sideName);
    }
  }

  /**
   * Reads the functions map from the config json.
   *
   * @param functionsMap The node containing the functions map
   * @throws IOException If parsing an URI fails
   */
  private void processJavaFunctions(JsonNode functionsMap) throws IOException {
    // Iterate over every function name
    for (Iterator<String> it = functionsMap.fieldNames(); it.hasNext(); ) {
      // Extract key and value
      String functionName = it.next();
      JsonNode functionNode = functionsMap.get(functionName).requireNonNull();

      // Extract the repository and the artifact
      String repo = functionNode.get("repo").requireNonNull().asText();
      MavenArtifact artifact = new MavenArtifact(functionNode.get("version").requireNonNull().asText());

      // Extract the list of arguments passed to the executable, this is always required
      List<String> args = new ArrayList<>();
      JsonNode argsNode = functionNode.get("args");
      for (int i = 0; i < argsNode.size(); i++) {
        args.add(argsNode.path(i).requireNonNull().asText());
      }

      // Extract the list of jvm arguments passed the JVM executing the artifact, this is not required
      List<String> jvmArgs = new ArrayList<>();
      if (functionNode.has("jvmargs")) {
        // Found the jvmargs node, process it
        JsonNode jvmArgsNode = functionNode.get("jvmargs");
        for (int i = 0; i < jvmArgsNode.size(); i++) {
          jvmArgs.add(jvmArgsNode.path(i).requireNonNull().asText());
        }
      }

      URI repoURI;
      try {
        repoURI = new URI(repo);
      } catch(URISyntaxException e) {
        throw new IOException("Failed to parse " + repo + " as a URI", e);
      }

      // Add the function to the found functions
      javaFunctions.put(functionName, new JavaExecutionTemplate(repoURI, artifact, args, jvmArgs));
    }
  }

  /**
   * Reads the steps array of the given side and constructs its functions.
   *
   * @param stepsArray The json array to read the steps from
   * @param sideName   The name of the side this array belongs to
   */
  private void processSteps(JsonNode stepsArray, String sideName) {
    if (!stepsArray.isArray()) {
      throw new IllegalArgumentException("Expected steps of " + sideName + " to be an array");
    }

    // Construct the list of steps per side in place
    List<MCPFunction> sidedSteps = steps.compute(
        sideName,
        (k, v) -> {
          if (v == null) {
            return new ArrayList<>();
          } else {
            // processSteps has been called twice with the same sideName
            throw new IllegalStateException("Steps for side " + sideName + " processed already");
          }
        }
    );

    // Iterate every step
    for (int i = 0; i < stepsArray.size(); i++) {
      JsonNode stepNode = stepsArray.path(i);

      // Collect the input values of the step
      Map<String, String> values = new HashMap<>();
      for (Iterator<String> it = stepNode.fieldNames(); it.hasNext(); ) {
        String key = it.next();
        values.put(key, stepNode.get(key).requireNonNull().asText());
      }

      String type = values.get("type");
      if (type == null) {
        // Every step requires at least a type value
        throw new IllegalArgumentException("Missing type value for step");
      } else if (shouldIgnoreStep(type)) {
        continue;
      }

      // Some steps don't have a name value, the type is then used as a name
      String name = values.getOrDefault("name", type);

      // Some steps don't have an input
      String input = values.containsKey("input") ?
          resolveVariableValue(values.get("input"), sideName, values) : null;
      if (input != null) {
        // An input was found, rewrite the value in case it was a variable and has been resolved
        values.put("input", input);
      }

      if (javaFunctions.containsKey(type)) {
        Path output = createOutput(sideName, name, "jar");
        values.put("output", output.toString());

        // This step is a Java function, construct the execution
        JavaExecutionTemplate template = javaFunctions.get(type);

        // Construct the arguments replacing variables with their respective values
        List<String> args = new ArrayList<>();
        for (String arg : template.getArgs()) {
          args.add(resolveVariableValue(arg, sideName, values));
        }

        // Construct the execution function
        JavaExecutionFunction function = new JavaExecutionFunction(
            name,
            output,
            template.getExecutionArtifact(),
            utilities.getHttpClient() == null ? null :
                new RemoteMavenRepository(
                    utilities.getHttpClient(),
                    template.getExecutionArtifactRepo()
                ),
            args,
            template.getJvmArgs()
        );

        sidedSteps.add(function);
        continue;
      }

      switch (type) {
        case "inject": {
          if (input == null) {
            throw new IllegalArgumentException("The inject function always requires an input");
          }

          Path output = createOutput(sideName, name, "jar");
          values.put("output", output.toString());

          // Construct the inject function
          InjectFunction function = new InjectFunction(
              name,
              Paths.get(input),
              output
          );
          sidedSteps.add(function);
          break;
        }

        case "strip": {
          if (input == null) {
            throw new IllegalArgumentException("The strip function always requires an input");
          }

          if (!variables.containsKey("mappings")) {
            // This will hopefully never happen
            throw new IllegalArgumentException("The strip functions requires mappings to be supplied");
          }

          Path output = createOutput(sideName, name, "jar");
          values.put("output", output.toString());

          // Construct the strip function
          StripFunction function = new StripFunction(
              name,
              variables.get("mappings"),
              Paths.get(input),
              output,
              // It is not clear if this value ever appears, but the MCP checks for it, so do we
              // By default the mode is always whitelist
              resolveVariableValue(
                  values.getOrDefault("mode", "whitelist"),
                  sideName,
                  values
              ).equalsIgnoreCase("whitelist")
          );

          sidedSteps.add(function);
          break;
        }

        case "patch": {
          if (input == null) {
            throw new IllegalArgumentException("The patch function always requires an input");
          }

          Path output = createOutput(sideName, name, "jar");
          values.put("output", output.toString());

          // Resolve the patches input
          Path patches = Paths.get(resolveVariableValue("{patches}", sideName, values));

          // Construct the patch function
          PatchFunction function = new PatchFunction(
              name,
              Paths.get(input),
              output,
              patches
          );

          sidedSteps.add(function);
          break;
        }

        case "listLibraries": {
          Path output = createOutput(sideName, name, "txt");
          values.put("output", output.toString());

          // Construct the listLibraries function
          ListLibrariesFunction function = new ListLibrariesFunction(
              name,
              output,
              clientJar
          );

          sidedSteps.add(function);
          break;
        }

        default: {
          throw new IllegalArgumentException("Got task " + name + " of type " + type + " which is neither " +
              "a builtin function nor defined via the java functions");
        }
      }
    }
  }

  /**
   * Resolves a path relative to the MCP config directory.
   *
   * @param partial The partial path
   * @return A path resolved to the MCP config directory
   */
  private Path resolveConfigPath(String partial) {
    return mcpPath.resolve(partial);
  }

  /**
   * Creates the output path with the associated variable for the given step.
   *
   * @param stepName      The name of the step to create the output for
   * @param side          The side the output belongs to
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
   * @param input          The variable name or value of the variable itself. Will be treated as a variable if starts with
   *                       <code>{</code> and ends with <code>}</code>.
   * @param side           The side the variable is being resolved for
   * @param extraVariables Extra variables to resolves values from
   * @return The resolved variable, or {@code null}, if the input name was null
   * @throws IllegalArgumentException If the input is a variable but no does not exist
   */
  private String resolveVariableValue(String input, String side, Map<String, String> extraVariables) {
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
   * @param input          The variable name or value of the variable itself. Will be treated as a variable if starts with
   *                       <code>{</code> and ends with <code>}</code>.
   * @param side           The side the variable is being resolved for
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
        throw new IllegalArgumentException("Variable " + input + " (sided " + sidedName + ") does not exist");
      }

      // The variable is a sided one
      return variables.get(sidedName).toString();
    }

    return variables.get(input).toString();
  }

  /**
   * Determines if the given step type should be ignored.
   *
   * @param stepType The type of the step to check
   * @return {@code true} if the steps of the given type should be ignored, {@code false} otherwise
   */
  private boolean shouldIgnoreStep(String stepType) {
    switch (stepType) {
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

  /**
   * Prepares all steps for of the given side for execution.
   *
   * @param side The side to prepare the steps for
   * @throws DeobfuscationException If a step fails to prepare
   */
  public void prepare(String side) throws DeobfuscationException {
    if (!steps.containsKey(side)) {
      throw new IllegalArgumentException("No steps defined for side " + side);
    }

    List<MCPFunction> sidedSteps = steps.get(side);

    // Iterate over every step of the given side
    for (MCPFunction step : sidedSteps) {
      Path output = step.getOutput();

      if (!Files.isRegularFile(output)) {
        Path outputDir = output.getParent();
        if (!Files.isDirectory(outputDir)) {
          // If the output directory does not exist, create it
          try {
            Files.createDirectories(outputDir);
          } catch (IOException e) {
            throw new DeobfuscationException("Failed to create output directory for step " + step.getName() +
                " for " + side);
          }
        }

        // The output does not exist, prepare the step for execution
        LOGGER.lifecycle("Preparing MCP step {} for {}", step.getName(), side);
        step.prepare(utilities);
      }
    }
  }

  /**
   * Runs all steps for the given side driving the run to completion
   *
   * @param side The side to execute the steps of
   * @return The output of the last step
   * @throws DeobfuscationException If a step fails to run
   */
  public Path execute(String side) throws DeobfuscationException {
    if (!steps.containsKey(side)) {
      throw new IllegalArgumentException("No steps defined for side " + side);
    }

    List<MCPFunction> sidedSteps = steps.get(side);

    boolean forceFollowingSteps = false;

    // Iterate over every step of the given side
    for (int i = 0; i < sidedSteps.size(); i++) {
      MCPFunction step = sidedSteps.get(i);
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
        LOGGER.lifecycle("[{}/{}] Running MCP step {} for {}", i + 1, sidedSteps.size(), step.getName(), side);
        long startMillis = System.currentTimeMillis();
        try {
          step.execute(utilities);
        } catch (DeobfuscationException | RuntimeException e) {
          try {
            // If an exception occurred, try to delete the file
            Files.deleteIfExists(output);
          } catch (IOException innerException) {
            LOGGER.error(
                "Failed to delete output after step failed, please manually clear the cache!",
                innerException
            );
          }
          throw e;
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
        LOGGER.lifecycle("[{}/{}] Skipping MCP step {} for {}, output exists already",
            i + 1, sidedSteps.size(), step.getName(), side);
      }
    }

    return sidedSteps.get(sidedSteps.size() - 1).getOutput();
  }
}
