package net.labyfy.gradle.java.exec;

/**
 * Represents the result of a java execution.
 */
public class JavaExecutionResult {
    private final int exitCode;
    private final String stdout;
    private final String stderr;

    /**
     * Constructs a new {@link JavaExecutionResult} with the given exit code, process standard output and process
     * standard error.
     *
     * @param exitCode The exit code of the process
     * @param stdout The standard output of the process
     * @param stderr The standard error of the process
     */
    public JavaExecutionResult(int exitCode, String stdout, String stderr) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    /**
     * Retrieves the exit code of the process.
     *
     * @return The exit code of the process
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Retrieves the standard output of the process.
     *
     * @return The standard output of the process
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * Retrieves the standard error of the process.
     *
     * @return The standard error of the process
     */
    public String getStderr() {
        return stderr;
    }
}
