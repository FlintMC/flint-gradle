package net.flintmc.gradle.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import net.flintmc.gradle.json.JsonConverter;
import net.flintmc.gradle.json.JsonConverterException;
import net.flintmc.gradle.property.FlintPluginProperties;
import net.flintmc.gradle.property.FlintPluginProperty;
import net.flintmc.installer.impl.repository.models.PackageModel;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.gradle.api.Project;
import org.gradle.api.credentials.HttpHeaderCredentials;
import org.gradle.api.file.FileCollection;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
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
   * Opens a stream to read from the given URL.
   *
   * @param client The {@link OkHttpClient} to use for opening the connection
   * @param uri    The URI to open
   * @return An input stream to read the data from
   * @throws IOException If an I/O error occurs while opening the connection
   */
  public static InputStream getURLStream(OkHttpClient client, URI uri) throws IOException {
    return getURLStream(client, uri, null);
  }

  /**
   * Opens a stream to read from the given URL.
   *
   * @param client  The {@link OkHttpClient} to use for opening the connection
   * @param uri     The URI to open
   * @param project The project to use for resolving authentication, or {@code null}, if authentication can be ignored
   * @return An input stream to read the data from
   * @throws IOException If an I/O error occurs while opening the connection
   */
  public static InputStream getURLStream(OkHttpClient client, URI uri, Project project) throws IOException {
    if (uri.getScheme().equals("jar") || uri.getScheme().equals("file")) {
      return uri.toURL().openStream();
    } else {
      Request.Builder requestBuilder = new Request.Builder()
          .url(uri.toString())
          .get();

      if (project != null) {
        URI distributorURI = FlintPluginProperties.DISTRIBUTOR_URL.resolve(project);
        if (distributorURI.getHost().equals(uri.getHost())) {
          // Reaching out to the distributor, add the authorization
          HttpHeaderCredentials credentials = getPublishCredentials(project, false);
          if (credentials != null) {
            requestBuilder.header(credentials.getName(), credentials.getValue());
          }
        }
      }

      Response response = client.newCall(requestBuilder.build()).execute();

      if (response.code() != 200) {
        throw new IOException("Failed to download file from " + uri + ", server responded with "
            + response.code() + " (" + response.message() + ")");
      }

      return response.body().byteStream();
    }
  }

  /**
   * Downloads the given URI to the given path. The parent directories are created as required.
   *
   * @param client  The {@link OkHttpClient} to use for downloading
   * @param uri     The URI to download
   * @param output  The target path
   * @param options Options specifying how to handle conflicts and symlinks
   * @throws IOException If the file can't be downloaded or created
   */
  public static void download(OkHttpClient client, URI uri, Path output, CopyOption... options) throws IOException {
    download(client, uri, output, null, options);
  }

  /**
   * Downloads the given URI to the given path. The parent directories are created as required.
   *
   * @param client  The {@link HttpClient} to use for downloading
   * @param uri     The URI to download
   * @param output  The target path
   * @param project The project to use for resolving authentication, or {@code null}, if authentication can be ignored
   * @param options Options specifying how to handle conflicts and symlinks
   * @throws IOException If the file can't be downloaded or created
   */
  public static void download(
      OkHttpClient client, URI uri, Path output, Project project, CopyOption... options) throws IOException {
    if (!Files.isDirectory(output.getParent())) {
      Files.createDirectories(output.getParent());
    }

    try (InputStream stream = getURLStream(client, uri, project)) {
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

  /**
   * Creates a new temporary directory.
   *
   * @return The path of the created directory
   * @throws IOException If an I/O error occurs while creating the directory
   */
  public static Path temporaryDir() throws IOException {
    return Files.createTempDirectory("java_compile");
  }

  /**
   * Recursively deletes the given directory.
   *
   * @param toNuke         The directory to delete
   * @param ignoreFailures If {@code true}, failures of deleting single files are ignored, exceptions when closing the
   *                       stream are still thrown
   * @throws IOException If an I/O error occurs while closing the stream, or while deleting files if ignoreFailures is
   *                     false
   */
  public static void nukeDirectory(Path toNuke, boolean ignoreFailures) throws IOException {
    // Walk all files in the given dir
    try (Stream<Path> allFiles = Files.walk(toNuke)) {
      // Sort them so the files come before the directories
      allFiles.sorted(Comparator.reverseOrder()).forEach((path) -> {
        try {
          // Delete the single file
          Files.delete(path);
        } catch (IOException e) {
          if (!ignoreFailures) {
            // If failures should not be ignore, throw an unchecked IO exception which will
            // be caught by the block later down
            throw new UncheckedIOException(e);
          }
        }
      });
    } catch (UncheckedIOException e) {
      // Rethrow the cause of the exception which has been thrown above
      throw e.getCause();
    }
  }

  /**
   * Safely concatenates paths to an URI.
   *
   * @param base  The URI to concatenate to
   * @param paths The paths to concatenate
   * @return The new URI
   */
  public static URI concatURI(URI base, String... paths) {
    URI current = base;

    for (String path : paths) {
      while (path.startsWith("/")) {
        path = path.substring(1);
      }
      current = current.resolve(current.getPath() + '/' + path);
    }

    return current;
  }

  /**
   * Used for completely erasing the type of a variable. Sometimes Java does not allow casting back target type easily,
   * especially when a wildcard is used. In this case this method may help to force the type to fit.
   * <p>
   * <b>Use with care! This essentially circumvents all type checking, you have been warned!</b>
   *
   * @param in  The object to cast
   * @param <T> The type to cast to
   * @return in
   */
  @SuppressWarnings("unchecked")
  public static <T> T forceCast(Object in) {
    return (T) in;
  }

  /**
   * Checks if a file is a package jar file.
   *
   * @param file The file to check
   * @return {@code true} if the jar is a package jar, {@code false} otherwise
   * @throws IOException If an I/O error occurs
   */
  public static boolean isPackageJar(File file) throws IOException {
    if (file.getName().endsWith(".jar")) {
      // Needs to be a jar file
      return false;
    }

    try (JarFile jarFile = new JarFile(file)) {
      return jarFile.getJarEntry("manifest.json") != null;
    }
  }

  /**
   * Tries to read the package model from a jar file.
   *
   * @param file The file to read the package model from
   * @return The read package model, or {@code null}, if the file is not a jar or does not contain a manifest
   * @throws IOException            If an I/O error occurs
   * @throws JsonConverterException If the {@code manifest.json} can't be read as a {@link PackageModel}
   */
  public static PackageModel getPackageModelFromJar(File file) throws IOException, JsonConverterException {
    if (file.getName().endsWith(".jar")) {
      // Needs to be a jar file
      return null;
    }

    try (JarFile jarFile = new JarFile(file)) {
      JarEntry entry = jarFile.getJarEntry("manifest.json");
      if (entry == null) {
        return null;
      }

      try (InputStream stream = jarFile.getInputStream(entry)) {
        return JsonConverter.PACKAGE_MODEL_SERIALIZER.fromString(readAll(stream), PackageModel.class);
      }
    }
  }

  /**
   * Retrieves the per project unique cache directory.
   *
   * @return The per project unique cache directory
   */
  public static File getProjectCacheDir(Project project) {
    return new File(project.getProjectDir(), ".flint/" + md5Hex(project.getPath().getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * Prints a deprecation warning with a stacktrace.
   *
   * @param project The project executing the deprecated thing
   * @param message The message to print
   */
  public static void nagDeprecated(Project project, String message) {
    project.getLogger().warn("Deprecation warning:", new Throwable(message));
  }

  /**
   * Creates a file collection with all duplicates removed.
   *
   * @param project     The project to create the file collection with
   * @param collections The collections to combine to a new collection without duplicates
   * @return The collection without any duplicates
   */
  public static FileCollection deduplicatedFileCollection(Project project, FileCollection... collections) {
    Set<File> files = new HashSet<>();

    // Iterate all collections
    for (FileCollection collection : collections) {
      if (collection == null) {
        // Skip collections which are null
        continue;
      }

      for (File file : collection.getFiles()) {
        // Make sure every path is absolute to reliably detect duplicates
        files.add(file.getAbsoluteFile());
      }
    }

    // Use the project to create a new file collection out of a set of files
    return project.files(files);
  }

  /**
   * Reads an entire stream as UTF-8.
   *
   * @param stream The stream to read
   * @return The read data as UTF-8
   * @throws IOException If an I/O error occurs while reading
   */
  public static String readAll(InputStream stream) throws IOException {
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      copyStream(stream, out);
      return out.toString("UTF-8");
    }
  }

  /**
   * Retrieves the HTTP header credentials used for publishing.
   *
   * @param project              The project to use for resolving the properties
   * @param required             If {@code true}, this method will abort the build if no authorization is configured
   * @param notAvailableSolution Messages to display as a solution in case the credentials can't be computed
   * @return The HTTP header credentials used for publishing
   */
  public static HttpHeaderCredentials getPublishCredentials(
      Project project, boolean required, String... notAvailableSolution) {
    HttpHeaderCredentials publishCredentials = project.getObjects().newInstance(HttpHeaderCredentials.class);

    // Retrieve either a bearer or publish token
    String bearerToken = FlintPluginProperties.DISTRIBUTOR_BEARER_TOKEN.resolve(project);
    if (bearerToken != null) {
      publishCredentials.setName("Authorization");
      publishCredentials.setValue("Bearer " + bearerToken);
    } else {
      FlintPluginProperty<String> publishTokenProperty = FlintPluginProperties.DISTRIBUTOR_PUBLISH_TOKEN;
      String publishToken = required ?
          publishTokenProperty.require(project, notAvailableSolution) :
          publishTokenProperty.resolve(project);

      if (publishToken == null) {
        return null;
      }

      publishCredentials.setName("Publish-Token");
      publishCredentials.setValue(publishToken);
    }

    return publishCredentials;
  }

  /**
   * Writes all remaining data of an {@link InputStream} to a byte array.
   *
   * @param stream the stream to read from
   * @return the written byte array
   * @throws IOException If the first byte cannot be read for any reason
   *                     other than the end of the file, if the input stream has been closed, or
   *                     if some other I/O error occurs.
   */
  public static byte[] toByteArray(InputStream stream) throws IOException {
    byte[] data = new byte[stream.available()];
    stream.read(data);
    return data;
  }

  /**
   * Writes a given byte array as a hex string.
   *
   * @param data the data to convert
   * @return the data as a hex string
   */
  public static String md5Hex(byte[] data) {
    try {
      return new HexBinaryAdapter().marshal(MessageDigest.getInstance("MD5").digest(data));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("MD5 digest not available");
    }
  }
}
