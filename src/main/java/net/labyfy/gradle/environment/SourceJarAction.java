package net.labyfy.gradle.environment;

/**
 * Base interface for all actions which can be taken on source jars.
 */
public interface SourceJarAction {
    /**
     * Processes a source code snippet.
     *
     * @param source The snippet to process
     */
    void process(StringBuffer source);
}
