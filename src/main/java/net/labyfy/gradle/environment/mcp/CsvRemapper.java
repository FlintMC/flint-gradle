package net.labyfy.gradle.environment.mcp;

import net.labyfy.gradle.util.Util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for remapping source jars using CSV mappings.
 */
public class CsvRemapper {
    private static final Pattern SEARGE_PATTERN =
            Pattern.compile("func_[0-9]+_[a-zA-Z_]+|field_[0-9]+_[a-zA-Z_]+|p_[\\w]+_\\d+_\\b");


    private final Map<String, String> names;

    /**
     * Constructs a new, empty {@link CsvRemapper}.
     */
    public CsvRemapper() {
        names = new HashMap<>();
    }

    /**
     * Loads the given CSV file as mappings.
     *
     * @param file The file to load
     * @throws IOException If an I/O error occurs while loading the given file
     */
    public void loadCsv(Path file) throws IOException  {
        // Extract all lines from the file
        List<String> lines = Files.readAllLines(file);

        if(lines.isEmpty()) {
            throw new IOException("Invalid, empty CSV file " + file.toString());
        }

        int keyIndex = -1;
        int nameIndex = -1;

        // Iterate every line
        for(String line : lines) {
            // MCP files uses , as a separator
            String[] parts = line.split(",");

            if(keyIndex == -1) {
                // First row
                if(parts.length < 2) {
                    // MCP mappings CSV's always have more than 1 row
                    throw new IOException("Invalid mappings CSV, contains less than 2 rows");
                }

                // Iterate the header row parts
                for(int i = 0; i < parts.length; i++) {
                    String part = parts[i];
                    if(part.equals("searge") || part.equals("param")) {
                        // Found the field defining the key
                        if(keyIndex != -1) {
                            throw new IOException("Invalid CSV, contains duplicated searge or param row");
                        }

                        keyIndex = i;
                    } else if(part.equals("name")) {
                        // Found the field defining the value
                        if(nameIndex != -1) {
                            throw new IOException("Invalid CSV, contains duplicated name row");
                        }

                        nameIndex = i;
                    }
                }

                // Check if all required fields have been set
                if(keyIndex == -1) {
                    throw new IOException("Invalid mappings CSV, does not contain searge or param row");
                } else if(nameIndex == -1) {
                    throw new IOException("Invalid mappings CSV, does not contain name row");
                }
            } else {
                // Check if the row is valid
                if(keyIndex >= parts.length || nameIndex >= parts.length) {
                    throw new IOException("Invalid mappings CSV, line " + line + " does not contain enough fields");
                }

                names.put(parts[keyIndex], parts[nameIndex]);
            }
        }
    }

    /**
     * Remaps the given jar file.
     *
     * @param input The path to the jar to remap
     * @param output The path to the jar to write the remapped sources to
     * @throws IllegalStateException If {@link #loadCsv(Path)} has not been called at least once to load mappings
     * @throws IOException If an I/O error occurs while remapping the files
     */
    public void remapSourceJar(Path input, Path output) throws IOException {
        if(names.isEmpty()) {
            throw new IllegalStateException("No mappings have been added to this remapper");
        }

        try(
                ZipInputStream inputStream = new ZipInputStream(Files.newInputStream(input));
                ZipOutputStream outputStream = new ZipOutputStream(Files.newOutputStream(output))
        ) {
            ZipEntry entry;
            // Iterate over every entry
            while ((entry = inputStream.getNextEntry()) != null) {
                outputStream.putNextEntry(entry);

                // We can only remap java files
                if(entry.getName().endsWith(".java")) {
                    // Remap the given java file
                    remapStream(inputStream, outputStream);
                } else {
                    // Nothing to do, just copy the entire stream
                    Util.copyStream(inputStream, outputStream);
                }

                // Make sure to close the entry to finish it
                outputStream.closeEntry();
            }
        }
    }

    /**
     * Remaps the given input stream to the given output stream.
     *
     * @param inputStream The stream to read the data to remap from
     * @param outputStream The stream to write the remapped data to
     * @throws IOException If an I/O error occurs while remapping
     */
    private void remapStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        // Create a reader for reading line by line
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        StringBuffer lineBuilder = new StringBuffer();
        String line;

        // Read all lines available
        while ((line = reader.readLine()) != null) {
            // Make sure to reset the buffer for every line
            lineBuilder.setLength(0);

            // Apply the pattern matcher to the line
            Matcher matcher = SEARGE_PATTERN.matcher(line);
            while (matcher.find()) {
                // Found a searge name, remap it
                String mcpName = names.get(matcher.group());

                // At this point mcpName is either a deobfuscated name or null,
                // so replace it with either the mcpName or itself
                matcher.appendReplacement(lineBuilder, mcpName == null ? matcher.group() : mcpName);
            }

            // Append the non matching part
            matcher.appendTail(lineBuilder);

            // Write the replaced string to the output
            outputStream.write(lineBuilder.toString().getBytes(StandardCharsets.UTF_8));
            outputStream.write('\n');
        }
    }
}
