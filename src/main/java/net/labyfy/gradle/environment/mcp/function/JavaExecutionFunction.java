package net.labyfy.gradle.environment.mcp.function;

import net.labyfy.gradle.environment.DeobfuscationException;
import net.labyfy.gradle.environment.DeobfuscationUtilities;
import net.labyfy.gradle.java.exec.JavaExecutionHelper;
import net.labyfy.gradle.java.exec.JavaExecutionResult;
import net.labyfy.gradle.maven.MavenArtifactDownloader;
import net.labyfy.gradle.maven.MavenResolveException;
import net.labyfy.gradle.maven.ReadableMavenRepository;
import net.labyfy.gradle.maven.SimpleMavenRepository;
import net.labyfy.gradle.maven.pom.MavenArtifact;
import org.apache.http.client.HttpClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JavaExecutionFunction extends MCPFunction {
    private final MavenArtifact executionArtifact;
    private final ReadableMavenRepository artifactRepository;
    private final List<String> args;
    private final List<String> jvmArgs;

    /**
     * Constructs a new java execution function with the given name, input and output.
     *
     * @param name   The name of the function
     * @param output The output of the function
     */
    public JavaExecutionFunction(
            String name,
            Path output,
            MavenArtifact executionArtifact,
            ReadableMavenRepository artifactRepository,
            List<String> args,
            List<String> jvmArgs
    ) {
        super(name, output);
        this.executionArtifact = executionArtifact;
        this.artifactRepository = artifactRepository;
        this.args = args;
        this.jvmArgs = jvmArgs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepare(DeobfuscationUtilities utilities) throws DeobfuscationException {
        SimpleMavenRepository localRepository = utilities.getInternalRepository();
        if(!localRepository.isInstalled(executionArtifact)) {
            // Retrieve utilities
            MavenArtifactDownloader downloader = utilities.getDownloader();

            // The artifact is not installed already, install it
            if(artifactRepository == null) {
                // Can't download anything in offline mode
                throw new DeobfuscationException("Missing artifact " + executionArtifact + " in local repository, " +
                        "but working in offline mode");
            }

            boolean setupSource = !downloader.hasSource(artifactRepository);
            if(setupSource) {
                // The download has the source not set already, add it now
                downloader.addSource(artifactRepository);
            }

            try {
                // Install the artifact including dependencies
                downloader.installAll(executionArtifact, localRepository, true);
            } catch (IOException | MavenResolveException e) {
                throw new DeobfuscationException("Failed to install execution artifact", e);
            }

            if(setupSource) {
                // We added the source, clean up afterwards
                downloader.removeSource(artifactRepository);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(DeobfuscationUtilities utilities) throws DeobfuscationException {
        // Retrieve the required utilities
        JavaExecutionHelper executionHelper = utilities.getJavaExecutionHelper();

        // Locate the installed jar
        Path jarPath = utilities.getInternalRepository().getArtifactPath(executionArtifact);

        JavaExecutionResult result;
        try {
            // Execute the process
            result = executionHelper.execute(
                    jarPath,
                    output.getParent(),
                    args,
                    jvmArgs
            );
        } catch (IOException e) {
            // This happens if the execution fails, but has nothing to do with the exit code
            throw new DeobfuscationException("Failed to execute java process for function " + name);
        }

        if(result.getExitCode() != 0) {
            // The process failed to execute properly
            Path standardOutput;
            try {
                // Try to save the standard output
                standardOutput = saveOutput("stdout", result.getStdout());
            } catch (IOException e) {
                throw new DeobfuscationException("Failed to save standard output after process failed, " +
                        "some is very wrong", e);
            }

            Path standardError;
            try {
                // Try to save the standard error
                standardError = saveOutput("stderr", result.getStderr());
            } catch (IOException e) {
                throw new DeobfuscationException("Failed to save standard error after process failed, " +
                        "some is very wrong", e);
            }

            throw new DeobfuscationException("Process failed with exit code " + result.getExitCode() +
                    " for function " + name + ", logs can be found here: \n" +
                    "\tstderr: " + standardError.toString() + "\n" +
                    "\tstdout: " + standardOutput.toString());
        }
    }

    /**
     * Saves the given string to a temporary file.
     *
     * @param outputName The name to append to the temporary files suffix
     * @param outputContent The content to write into the file
     * @return The path to the temporary file
     * @throws IOException If an I/O error occurs while writing the file
     */
    private Path saveOutput(String outputName, String outputContent) throws IOException {
        Path tempFile = Files.createTempFile("labyfy_gradle_", "mcp_function_" + name + "_" + outputName + ".log");
        Files.write(tempFile, outputContent.getBytes());
        return tempFile;
    }
}
