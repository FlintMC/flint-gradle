package net.labyfy.gradle.java.exec;

import org.gradle.api.Project;
import org.gradle.api.tasks.JavaExec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Utility class for executing java with the gradle configured JVM.
 */
public class JavaExecutionHelper {
    private final Project project;

    /**
     * Constructs a new {@link JavaExecutionHelper} using the given project as a JVM execution source.
     *
     * @param project The project to take the JVM from
     */
    public JavaExecutionHelper(Project project) {
        this.project = project;
    }

    /**
     * Executes the given jar with the given arguments and JVM arguments.
     *
     * @param jar         The jar to execute
     * @param workingDir  The directory to execute the jar in
     * @param programArgs The arguments to pass to the jar
     * @param jvmArgs     The arguments to pass to the JVM
     * @return The result of the execution
     * @throws IOException If an I/O error occurs executing the jar
     */
    @SuppressWarnings("UnstableApiUsage")
    public JavaExecutionResult execute(Path jar, Path workingDir, List<String> programArgs, List<String> jvmArgs)
            throws IOException {
        String mainClass = determineMainClass(jar);

        if(!Files.isDirectory(workingDir)) {
            // Make sure the working directory exists
            Files.createDirectories(workingDir);
        }

        // Use the project to create a java exec task. This will preserve the JVM gradle was invoked with
        JavaExec task = project.getTasks().create("javaExec" + UUID.randomUUID(), JavaExec.class);

        try (
                // Capture the output
                ByteArrayOutputStream standardOutput = new ByteArrayOutputStream();
                ByteArrayOutputStream standardError = new ByteArrayOutputStream()
        ) {
            // Configure output capturing
            task.setStandardOutput(standardOutput);
            task.setErrorOutput(standardError);

            // Give the arguments to gradle
            task.setJvmArgs(jvmArgs);
            task.setArgs(programArgs);

            // Configure the execution parameters
            task.setClasspath(project.files(jar));
            task.setMain(mainClass);
            task.setWorkingDir(project.file(workingDir));

            // Don't error if the exit value is not 0
            task.setIgnoreExitValue(true);

            // Invoke the execution
            task.exec();

            // Collect results and return them to the caller
            int exitValue = task.getExecutionResult().get().getExitValue();
            return new JavaExecutionResult(
                    exitValue,
                    new String(standardOutput.toByteArray()),
                    new String(standardError.toByteArray())
            );
        } finally {
           task.setEnabled(false);
        }
    }

    /**
     * Determines the main class of the given jar file.
     *
     * @param jar Path to the jar file to determine the main class of
     * @return The main class of the given jar file
     * @throws IOException If an I/O error occurs while determining the main class
     */
    private String determineMainClass(Path jar) throws IOException {
        // Open the file as a jar so we can access its attributes
        try(JarFile jarFile = new JarFile(project.file(jar))) {
            Manifest manifest = jarFile.getManifest();
            if(manifest == null) {
                // Jar contains no manifest
                throw new IOException("Unable to determine main class of " + jar.toString() + ", no manifest found");
            }

            // The Main-Class can always be found in the main attributes
            Attributes mainAttributes = manifest.getMainAttributes();
            if(mainAttributes == null) {
                throw new IOException("Unable to determine main class of " + jar.toString() +
                        ", manifest does not contain main entries");
            }

            String mainClass = mainAttributes.getValue(Attributes.Name.MAIN_CLASS);
            if(mainClass == null || mainClass.isEmpty()) {
                // Main-Class attribute is not present or set to an empty string
                throw new IOException("Unable to determine main class of " + jar.toString() + ", Main-Class not set");
            }

            return mainClass;
        }
    }
}
