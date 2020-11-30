package net.flintmc.gradle.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import net.flintmc.gradle.json.JsonConverter;
import net.flintmc.gradle.json.JsonConverterException;
import net.flintmc.installer.impl.repository.models.InternalModelSerializer;
import net.flintmc.installer.impl.repository.models.PackageModel;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.gradle.api.Project;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
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
   * Downloads the given url to the given path. The parent directories are created as required.
   *
   * @param client  The {@link HttpClient} to use for downloading
   * @param url     The url to download
   * @param output  The target path
   * @param options Options specifying how to handle conflicts and symlinks
   * @throws IOException If the file can't be downloaded or created
   */
  public static void download(HttpClient client, String url, Path output, CopyOption... options) throws IOException {
    if(!Files.isDirectory(output.getParent())) {
      Files.createDirectories(output.getParent());
    }

    HttpGet getRequest = new HttpGet(url);
    HttpResponse response = client.execute(getRequest);

    StatusLine status = response.getStatusLine();
    if(status.getStatusCode() != 200) {
      throw new IOException("Failed to download file from " + url + ", server responded with "
          + status.getStatusCode() + " (" + status.getReasonPhrase() + ")");
    }

    try(InputStream stream = response.getEntity().getContent()) {
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
    try(ZipFile zipFile = new ZipFile(zip.toFile())) {
      // Get a list of all entries
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while(entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if(entry.isDirectory()) {
          // Required directories will be created automatically
          continue;
        }

        String name = entry.getName();
        if(name.startsWith("/")) {
          // Make sure that the entry does not start with a /, else it will corrupt
          // the Path#resolve result
          name = name.substring(1);
        }

        Path targetFile = targetDir.resolve(name);
        if(!Files.exists(targetFile.getParent())) {
          // Make sure the parent directories exist
          Files.createDirectories(targetFile.getParent());
        }

        try(InputStream entryStream = zipFile.getInputStream(entry)) {
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
    while((count = in.read(buffer)) != -1) {
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
    while((line = reader.readLine()) != null) {
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
    for(String line : lines) {
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
    try(Stream<Path> allFiles = Files.walk(toNuke)) {
      // Sort them so the files come before the directories
      allFiles.sorted(Comparator.reverseOrder()).forEach((path) -> {
        try {
          // Delete the single file
          Files.delete(path);
        } catch(IOException e) {
          if(!ignoreFailures) {
            // If failures should not be ignore, throw an unchecked IO exception which will
            // be caught by the block later down
            throw new UncheckedIOException(e);
          }
        }
      });
    } catch(UncheckedIOException e) {
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

    for(String path : paths) {
      while(path.startsWith("/")) {
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
   * Serializes a collection into an {@link ObjectOutput}.
   *
   * @param collection   The collection to serialize
   * @param objectOutput The output to write to
   * @throws IOException If an I/O error occurs while writing
   */
  public static void serializeCollection(Collection<?> collection, ObjectOutput objectOutput) throws IOException {
    objectOutput.write(collection.size());

    for(Object value : collection) {
      objectOutput.writeObject(value);
    }
  }

  /**
   * Deserializes a collection from an {@link ObjectInput}.
   *
   * @param collection  The collection to deserialize
   * @param objectInput The input to read from
   * @throws IOException            If an I/O error occurs while reading
   * @throws ClassNotFoundException If the original collection contained a class which now can't be found
   */
  public static void deserializeCollection(Collection<?> collection, ObjectInput objectInput)
      throws IOException, ClassNotFoundException {
    int size = objectInput.readInt();

    for(int i = 0; i < size; i++) {
      Object value = objectInput.readObject();
      collection.add(forceCast(value)); // forceCast required, can't cast back to `?`
    }
  }

  /**
   * Converts an Object to a string or {@code "undefined"} if the object is null. Used to mimic Gradle's behavior.
   *
   * @param value The object to convert
   * @return The object as a string
   */
  public static String toStringOrUndefined(Object value) {
    return value == null ? "undefined" : value.toString();
  }

  /**
   * Checks if a file is a package jar file.
   *
   * @param file The file to check
   * @return {@code true} if the jar is a package jar, {@code false} otherwise
   * @throws IOException If an I/O error occurs
   */
  public static boolean isPackageJar(File file) throws IOException {
    if(file.getName().endsWith(".jar")) {
      // Needs to be a jar file
      return false;
    }

    try(JarFile jarFile = new JarFile(file)) {
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
    if(file.getName().endsWith(".jar")) {
      // Needs to be a jar file
      return null;
    }

    try(JarFile jarFile = new JarFile(file)) {
      JarEntry entry = jarFile.getJarEntry("manifest.json");
      if(entry == null) {
        return null;
      }

      try(InputStream stream = jarFile.getInputStream(entry)) {
        return JsonConverter.streamToObject(stream, PackageModel.class);
      }
    }
  }

  /**
   * Retrieves the per project unique cache directory.
   *
   * @return The per project unique cache directory
   */
  public static File getProjectCacheDir(Project project) {
    File buildDir = project.getBuildDir();
    return new File(buildDir, "flint/" + DigestUtils.md5Hex(project.getPath()));
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
}
