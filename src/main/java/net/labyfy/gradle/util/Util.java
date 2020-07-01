package net.labyfy.gradle.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    /**
     * Extracts the given zip file to the given directory.
     *
     * @param zip       The path to the zip file to extract
     * @param targetDir The directory to extract the zip file into
     * @param options   Options to pass to {@link Files#copy(InputStream, Path, CopyOption...)}
     * @throws IOException If an I/O error occurs while reading or writing files
     */
    public static void extractZip(Path zip, Path targetDir, CopyOption... options) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip.toFile())) {
            // Get a list of all entries
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    // Required directories will be created automatically
                    continue;
                }

                String name = entry.getName();
                if (name.startsWith("/")) {
                    // Make sure that the entry does not start with a /, else it will corrupt
                    // the Path#resolve result
                    name = name.substring(1);
                }

                Path targetFile = targetDir.resolve(name);
                if (!Files.exists(targetFile.getParent())) {
                    // Make sure the parent directories exist
                    Files.createDirectories(targetFile.getParent());
                }

                try (InputStream entryStream = zipFile.getInputStream(entry)) {
                    // Copy the entire entry to the target file
                    Files.copy(entryStream, targetFile, options);
                }
            }
        }
    }

    /**
     * Copies the given input stream to the given output stream.
     *
     * @param in  The input stream to copy from
     * @param out The output stream to copy to
     * @throws IOException If an I/O error occurs while copying
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];

        int count;
        while ((count = in.read(buffer)) != -1) {
            out.write(buffer, 0, count);
        }
    }

    /**
     * Reads all lines from the given input stream without closing it.
     *
     * @param in The stream to read all lines from
     * @return A list of all read lines
     * @throws IOException If an I/O error occurs while reading from the stream
     */
    public static List<String> readAllLines(InputStream in) throws IOException {
        List<String> lines = new ArrayList<>();

        // Get a buffered reader for the stream
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;

        // Read all lines until we reach the EOS
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }

        return lines;
    }

    /**
     * Writes the given lines to the given output stream.
     *
     * @param lines The lines to write
     * @param out   The stream to write the lines to
     * @throws IOException If an I/O exception occurs while writing to the stream
     */
    public static void writeAllLines(List<String> lines, OutputStream out) throws IOException {
        for (String line : lines) {
            out.write(line.getBytes(StandardCharsets.UTF_8));
            out.write('\n');
        }
    }
}
