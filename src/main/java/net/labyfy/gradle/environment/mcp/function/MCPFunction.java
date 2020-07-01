package net.labyfy.gradle.environment.mcp.function;

import net.labyfy.gradle.environment.DeobfuscationException;
import net.labyfy.gradle.environment.DeobfuscationUtilities;

import java.nio.file.Path;

/**
 * Base class for all MCP functions.
 */
public abstract class MCPFunction {
    protected final String name;
    protected final Path output;

    /**
     * Constructs a new MCP function with the given name and output.
     *
     * @param name The name of the function
     * @param output The output of the function
     */
    protected MCPFunction(String name, Path output) {
        this.name = name;
        this.output = output;
    }

    /**
     * Retrieves the name of this MCP function.
     *
     * @return The name of this MCP function
     */
    public String getName() {
        return name;
    }

    /**
     * Retrieves the output of this MCP function.
     *
     * @return The output of this MCP function
     */
    public Path getOutput() {
        return output;
    }

    /**
     * Prepares the given function for execution.
     *
     * @param utilities The utilities which can be used during preparation
     * @throws DeobfuscationException If the preparations fail
     */
    public void prepare(DeobfuscationUtilities utilities) throws DeobfuscationException {}

    /**
     * Executes this MCP function.
     *
     * @param utilities The utilities which can be used during execution
     * @throws DeobfuscationException If the execution if this MCP function fails
     */
    public abstract void execute(DeobfuscationUtilities utilities) throws DeobfuscationException;
}
