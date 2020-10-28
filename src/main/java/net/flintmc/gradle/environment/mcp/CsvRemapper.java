package net.flintmc.gradle.environment.mcp;

import net.flintmc.gradle.environment.SourceJarAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for remapping source jars using CSV mappings.
 */
public class CsvRemapper implements SourceJarAction {
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
  public void loadCsv(Path file) throws IOException {
    // Extract all lines from the file
    List<String> lines = Files.readAllLines(file);

    if (lines.isEmpty()) {
      throw new IOException("Invalid, empty CSV file " + file.toString());
    }

    int keyIndex = -1;
    int nameIndex = -1;

    // Iterate every line
    for (String line : lines) {
      // MCP files uses , as a separator
      String[] parts = line.split(",");

      if (keyIndex == -1) {
        // First row
        if (parts.length < 2) {
          // MCP mappings CSV's always have more than 1 row
          throw new IOException("Invalid mappings CSV, contains less than 2 rows");
        }

        // Iterate the header row parts
        for (int i = 0; i < parts.length; i++) {
          String part = parts[i];
          if (part.equals("searge") || part.equals("param")) {
            // Found the field defining the key
            if (keyIndex != -1) {
              throw new IOException("Invalid CSV, contains duplicated searge or param row");
            }

            keyIndex = i;
          } else if (part.equals("name")) {
            // Found the field defining the value
            if (nameIndex != -1) {
              throw new IOException("Invalid CSV, contains duplicated name row");
            }

            nameIndex = i;
          }
        }

        // Check if all required fields have been set
        if (keyIndex == -1) {
          throw new IOException("Invalid mappings CSV, does not contain searge or param row");
        } else if (nameIndex == -1) {
          throw new IOException("Invalid mappings CSV, does not contain name row");
        }
      } else {
        // Check if the row is valid
        if (keyIndex >= parts.length || nameIndex >= parts.length) {
          throw new IOException("Invalid mappings CSV, line " + line + " does not contain enough fields");
        }

        names.put(parts[keyIndex], parts[nameIndex]);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException If no mappings have been loaded with the {@link #loadCsv(Path)}
   */
  @Override
  public void process(StringBuffer line) {
    if (names.isEmpty()) {
      throw new IllegalStateException("No mappings have been loaded");
    }

    // Apply the pattern matcher to the line
    Matcher matcher = SEARGE_PATTERN.matcher(line.toString());

    // Clear the line so it can be rebuilt
    line.setLength(0);
    while (matcher.find()) {
      // Found a searge name, remap it
      String mcpName = names.get(matcher.group());

      // At this point mcpName is either a deobfuscated name or null,
      // so replace it with either the mcpName or itself
      matcher.appendReplacement(line, mcpName == null ? matcher.group() : mcpName);
    }

    // Append the non matching part
    matcher.appendTail(line);
  }
}
