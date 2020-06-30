package net.labyfy.gradle.maven;

/**
 * Exception representing a failure while resolving artifacts from a maven repository.
 */
public class MavenResolveException extends Exception {
    /**
     * Constructs a new {@link MavenResolveException} with the given message.
     *
     * @param message Message indicating what went wrong
     */
    public MavenResolveException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link MavenResolveException} with the given message and cause.
     *
     * @param message Message indicating what went wrong
     * @param cause The failure causing this exception
     */
    public MavenResolveException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link MavenResolveException} with the given cause.
     *
     * @param cause The failure causing this exception
     */
    public MavenResolveException(Throwable cause) {
        super(cause);
    }
}
