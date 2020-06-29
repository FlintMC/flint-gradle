package net.labyfy.gradle.json;

/**
 * Exception that may occur when converting data to a Json object.
 * Since this is just a wrapper exception, it always has a cause.
 */
public class JsonConverterException extends Exception {
    /**
     * Creates a new {@link JsonConverterException}.
     *
     * @param msg The message of the exception
     * @param t   The exception causing this exception
     */
    public JsonConverterException(String msg, Throwable t) {
        super(msg, t);
    }
}