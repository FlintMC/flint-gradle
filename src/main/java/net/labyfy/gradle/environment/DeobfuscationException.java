package net.labyfy.gradle.environment;

/**
 * Indicates a general failure while running deobfuscation.
 */
public class DeobfuscationException extends Exception {
    /**
     * Constructs a new {@link DeobfuscationException} with the given message.
     *
     * @param message The message explaining the failure
     */
    public DeobfuscationException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@link DeobfuscationException} with the given message and cause.
     *
     * @param message The message explaining the failure
     * @param cause The cause of this exception
     */
    public DeobfuscationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@link DeobfuscationException} with the given cause.
     *
     * @param cause The cause of this exception
     */
    public DeobfuscationException(Throwable cause) {
        super(cause);
    }
}
