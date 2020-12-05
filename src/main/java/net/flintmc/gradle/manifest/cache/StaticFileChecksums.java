package net.flintmc.gradle.manifest.cache;

import net.flintmc.gradle.util.Util;
import org.gradle.api.Project;

import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class StaticFileChecksums implements Externalizable {
  /**
   * Loads the cached static file checksums from a file.
   *
   * @param file The file to load the cached checksums from
   * @return The loaded checksums
   * @throws IOException If an I/O error occurs while loading the checksums
   */
  public static StaticFileChecksums load(File file) throws IOException {
    try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
      return Util.forceCast(in.readObject());
    } catch(ClassNotFoundException e) {
      throw new IOException("Failed to read cached checksums of static files due to ClassNotFoundException", e);
    }
  }

  private Map<URI, String> uriChecksums;
  private Map<File, String> fileChecksums;

  /**
   * Constructs a new {@link StaticFileChecksums} with all values empty.
   */
  public StaticFileChecksums() {
    this.uriChecksums = new HashMap<>();
    this.fileChecksums = new HashMap<>();
  }

  /**
   * Saves this instance to the given file.
   *
   * @param file The file to write the cache to
   * @throws IOException If an I/O error occurs
   */
  public void save(File file) throws IOException {
    try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
      out.writeObject(this);
    }
  }

  /**
   * Adds a checksum to the map of URI checksums.
   *
   * @param uri      The URI to add the checksum for
   * @param checksum The checksum of the download
   */
  public void add(URI uri, String checksum) {
    this.uriChecksums.put(uri, checksum);
  }

  /**
   * Checks if the checksums contain a checksum for the given URI.
   *
   * @param uri The URI to check for
   * @return {@code true} if a checksum for the URI has been cached, {@code false} otherwise
   */
  public boolean has(URI uri) {
    return uriChecksums.containsKey(uri);
  }

  /**
   * Retrieves the checksum for the given URI.
   *
   * @param uri The URI to retrieve the checksum for
   * @return The checksum of the download behind the URI, or {@code null}, if the URI has not been cached
   */
  public String get(URI uri) {
    return uriChecksums.get(uri);
  }

  /**
   * Clears all URI checksums.
   */
  public void clearURIs() {
    uriChecksums.clear();
  }

  /**
   * Adds a checksum to the map of static file checksums.
   *
   * @param file     The file to add the checksum for
   * @param checksum The checksum of the file
   */
  public void add(File file, String checksum) {
    this.fileChecksums.put(file, checksum);
  }

  /**
   * Checks if the checksums contain a checksum for the given file.
   *
   * @param file The file to check for
   * @return {@code true} if a checksum for the file has been cached, {@code false} otherwise
   */
  public boolean has(File file) {
    return fileChecksums.containsKey(file);
  }

  /**
   * Retrieves the checksum for the given file.
   *
   * @param file The file to retrieve the checksum for
   * @return The checksum for the given file, or {@code null}, if the file has not been cached
   */
  public String get(File file) {
    return fileChecksums.get(file);
  }

  /**
   * Clears all cached file checksums.
   */
  public void clearFiles() {
    fileChecksums.clear();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(this.uriChecksums);
    out.writeObject(this.fileChecksums);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    this.uriChecksums = Util.forceCast(in.readObject());
    this.fileChecksums = Util.forceCast(in.readObject());
  }

  /**
   * Retrieves a project unique file to cache bound dependencies in.
   *
   * @param project The project to retrieve the cache file for
   * @return The cache file
   */
  public static File getCacheFile(Project project) {
    return new File(Util.getProjectCacheDir(project), "static-file-checksums.bin");
  }
}
