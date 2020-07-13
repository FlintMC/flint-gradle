package net.labyfy.gradle.environment.mcp.function;

import net.labyfy.gradle.environment.DeobfuscationException;
import net.labyfy.gradle.environment.DeobfuscationUtilities;
import net.labyfy.gradle.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class InjectFunction extends MCPFunction {
    private final Path input;

    /**
     * Constructs a new Inject function with the given name, input and output.
     *
     * @param name   The name of the function
     * @param output The output of the function
     */
    public InjectFunction(String name, Path input, Path output) {
        super(name, output);
        this.input = input;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(DeobfuscationUtilities utilities) throws DeobfuscationException {
        try(
                ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(input));
                ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(output))
        ) {
            ZipEntry entry;

            // Iterate over every entry
            while ((entry = inputStream.getNextEntry()) != null) {
                // Copy the entry one to one
                outputStream.putNextEntry(entry);
                Util.copyStream(inputStream, outputStream);

                // Make sure to close the entry
                outputStream.closeEntry();
            }

            // Add our marker entry
            entry = new ZipEntry(".mcp-processed");
            outputStream.putNextEntry(entry);
            outputStream.closeEntry();
        } catch (IOException e) {
            throw new DeobfuscationException("Failed to execute inject function named " + name, e);
        }
    }
}
