package net.labyfy.gradle.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import net.labyfy.gradle.minecraft.data.version.VersionedRule;
import net.labyfy.gradle.minecraft.data.version.VersionedRuleAction;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Util {
    /**
     * Converts the given long into a byte array. Reverse operation of {@link #longFromByteArray(byte[])}.
     *
     * @param l The long to convert into a byte array
     * @return A 8 byte array containing the long
     */
    public static byte[] longToByteArray(long l) {
        return new byte[]{
                (byte) l,
                (byte) (l >> 8),
                (byte) (l >> 16),
                (byte) (l >> 24),
                (byte) (l >> 32),
                (byte) (l >> 40),
                (byte) (l >> 48),
                (byte) (l >> 56)
        };
    }

    /**
     * Converts the given byte array into a long. Reverse operation of {@link #longToByteArray(long)}.
     *
     * @param data The byte array to convert into a long
     * @return The reconstructed long
     */
    public static long longFromByteArray(byte[] data) {
        return ((long) data[7] << 56)
                | ((long) data[6] & 0xFF) << 48
                | ((long) data[5] & 0xFF) << 40
                | ((long) data[4] & 0xFF) << 32
                | ((long) data[3] & 0xFF) << 24
                | ((long) data[2] & 0xFF) << 16
                | ((long) data[1] & 0xFF) << 8
                | ((long) data[0] & 0xFF);
    }

    /**
     * Reads a {@link JsonNode} into an object using Jackson
     *
     * @param target The type of the target object
     * @param root   The node to convert into an object
     * @param ctxt   The context to use for error reporting and such
     * @param <T>    The type of the target object
     * @return The target object constructed from `root`
     * @throws IOException If any error occurs constructing the object
     */
    @SuppressWarnings("unchecked")
    public static <T> T readJsonValue(JavaType target, JsonNode root, DeserializationContext ctxt)
            throws IOException {
        JsonDeserializer<T> deserializer = (JsonDeserializer<T>) ctxt.findRootValueDeserializer(target);
        JsonParser parser = root.traverse(ctxt.getParser().getCodec());
        parser.nextToken();
        return deserializer.deserialize(parser, ctxt);
    }

    /**
     * Downloads the given url to the given path. The parent directories are
     * created as required.
     *
     * @param client  The {@link HttpClient} to use for downloading
     * @param url     The url to download
     * @param output  The target path
     * @param options Options specifying how to handle conflicts and symlinks
     * @throws IOException If the file can't be downloaded or created
     */
    public static void download(HttpClient client, String url, Path output, CopyOption... options) throws IOException {
        if (!Files.isDirectory(output.getParent())) {
            Files.createDirectories(output.getParent());
        }

        HttpGet getRequest = new HttpGet(url);
        HttpResponse response = client.execute(getRequest);

        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() != 200) {
            throw new IOException("Failed to download file from " + url + ", server responded with "
                    + status.getStatusCode() + " (" + status.getReasonPhrase() + ")");
        }

        try (InputStream stream = response.getEntity().getContent()) {
            Files.copy(stream, output, options);
        }
    }
}
