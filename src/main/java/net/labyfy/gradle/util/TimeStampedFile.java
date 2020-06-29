package net.labyfy.gradle.util;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Utility class for a file with a timestamp tracking the last update time.
 */
public class TimeStampedFile {
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeStampedFile.class);
    private final Path filePath;
    private final Path stampPath;

    /**
     * Constructs a new {@link TimeStampedFile} with the given path. The stamp file
     * will be created in the same directory and with the name of the original file
     * appended with {@code .stamp}.
     *
     * @param filePath The path of the real file
     */
    public TimeStampedFile(Path filePath) {
        this.filePath = filePath;
        this.stampPath = filePath.resolveSibling(filePath.getFileName() + ".stamp");
    }

    /**
     * Fetches the file from the remote backend if the local file is out of date.
     *
     * @param httpClient The client to use for fetching the file
     * @param updateUrl The URL to fetch the file from
     * @param dateTimeFormatter The date time formatter to use for parsing the Last-Modified Header
     *
     * @throws IOException If an I/O error occurs while updating the file
     */
    public void update(
            HttpClient httpClient, String updateUrl, DateTimeFormatter dateTimeFormatter) throws IOException {
        Date lastModifiedDate = null;

        // Send a HEAD request to the Mojang server in order to retrieve the Last-Modified header
        HttpHead headRequest = new HttpHead(updateUrl);
        HttpResponse headResponse = httpClient.execute(headRequest);
        StatusLine headStatus = headResponse.getStatusLine();
        if (headStatus.getStatusCode() != 200) {
            // Bail out of the server did not respond with 200-Ok
            throw new IOException("Server responded with status "
                    + headStatus.getStatusCode() + " (" + headStatus.getReasonPhrase() + ")");
        }

        Header[] lastModifiedHeaders = headResponse.getHeaders("Last-Modified");
        if (lastModifiedHeaders != null && lastModifiedHeaders.length > 0) {
            // We have received values in the Last-Modified header
            String timeValue = lastModifiedHeaders[0].getValue();
            if (timeValue != null) {
                // Reconstruct the local and remote date
                lastModifiedDate = Date.from(
                        dateTimeFormatter.parse(timeValue, ZonedDateTime::from).toInstant());
            } else {
                LOGGER.warn("Server sent an empty Last-Modified header, assuming " + filePath + " is out of date!");
            }
        } else {
            LOGGER.warn("Server did not send an Last-Modified header, assuming " + filePath + " is out of date!");
        }

        // Check if both the manifest file and the manifest time stamp exist
        if (Files.isRegularFile(this.filePath) && Files.isRegularFile(this.stampPath)
                && lastModifiedDate != null) {
            // Parse the local value from the stamp file
            long timestamp = Util.longFromByteArray(Files.readAllBytes(this.stampPath));

            Date lastLocalModifiedDate = Date.from(Instant.ofEpochMilli(timestamp));
            if (!lastLocalModifiedDate.before(lastModifiedDate)) {
                // Local file is newer
                return;
            }
        }

        // Fetch the file data
        HttpGet getRequest = new HttpGet(updateUrl);
        HttpResponse response = httpClient.execute(getRequest);

        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() != 200) {
            // Bail out if the server did not respond with 200-Ok
            throw new IOException("Server responded with status code "
                    + status.getStatusCode() + " (" + status.getReasonPhrase() + ")");
        }

        Path parentPath = this.filePath.getParent();
        if(!Files.isDirectory(parentPath)) {
            Files.createDirectories(parentPath);
        }

        if(lastModifiedDate != null) {
            // If the server sent a last modified date, save it
            Files.write(this.stampPath, Util.longToByteArray(lastModifiedDate.getTime()));
        } else {
            // We don't know when the file is from, delete the stamp file
            Files.deleteIfExists(this.stampPath);
        }

        // Write the received data
        Files.copy(response.getEntity().getContent(), this.filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Returns the NIO {@link Path} representation of this file.
     *
     * @return The path representation of this file
     */
    public Path toPath() {
        return this.filePath;
    }

    /**
     * Returns the IO {@link File} representation of this file.
     *
     * @return The java file representation of this file
     */
    public File toFile() {
        return this.filePath.toFile();
    }
}
