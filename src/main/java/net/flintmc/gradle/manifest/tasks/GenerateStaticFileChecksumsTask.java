package net.flintmc.gradle.manifest.tasks;

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.manifest.cache.StaticFileChecksums;
import net.flintmc.gradle.manifest.data.ManifestStaticFile;
import net.flintmc.gradle.manifest.data.ManifestStaticFileInput;
import net.flintmc.gradle.util.MaybeNull;
import net.flintmc.gradle.util.Util;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

/**
 * Task that generates and caches the checksums for static files.
 */
public class GenerateStaticFileChecksumsTask extends DefaultTask {
  private final OkHttpClient httpClient;
  private final ManifestStaticFileInput staticFiles;

  @Input
  private Set<URI> remoteFiles;

  @OutputFile
  private File cacheFile;

  /**
   * Constructs a new {@link GenerateStaticFileChecksumsTask}.
   *
   * @param httpClient  The HTTP client to use for downloading remote files
   * @param staticFiles The static files to generate checksums for
   */
  @Inject
  public GenerateStaticFileChecksumsTask(MaybeNull<OkHttpClient> httpClient, ManifestStaticFileInput staticFiles) {
    this.httpClient = httpClient.get();
    this.staticFiles = staticFiles;
  }

  /**
   * Retrieves the URI's of the remote files to compute the checksums for.
   *
   * @return The URI's to compute checksums for
   */
  public Set<URI> getRemoteFiles() {
    staticFiles.compute(getProject());

    if (remoteFiles == null) {
      remoteFiles = new HashSet<>();

      for (ManifestStaticFile remoteFile : staticFiles.getRemoteFiles()) {
        remoteFiles.add(remoteFile.getURI());
      }
    }

    return remoteFiles;
  }

  /**
   * Retrieves the local files to compute checksums for.
   *
   * @return The local files to compute checksums for
   */
  @InputFiles
  public Set<File> getLocalFiles() {
    staticFiles.compute(getProject());

    return staticFiles.getLocalFiles().keySet();
  }

  /**
   * Retrieves the cache file this task writes to.
   *
   * @return The cache file this task writes to
   */
  public File getCacheFile() {
    if (cacheFile == null) {
      cacheFile = StaticFileChecksums.getCacheFile(getProject());
    }

    return cacheFile;
  }

  /**
   * Generates and saves the checksums.
   *
   * @throws IOException If an I/O error occurs
   */
  @TaskAction
  public void generate() throws IOException {
    staticFiles.compute(getProject());

    File parentDir = getCacheFile().getParentFile();
    if (!parentDir.exists() && !parentDir.mkdirs()) {
      throw new IOException("Failed to create directory " + parentDir.getAbsolutePath());
    }

    StaticFileChecksums checksums = cacheFile.exists() ? StaticFileChecksums.load(cacheFile) : new StaticFileChecksums();

    // Calculate the checksums for local files
    checksums.clearFiles();
    for (File localFile : getLocalFiles()) {
      checksums.add(localFile, Util.md5Hex(Files.readAllBytes(localFile.toPath())));
    }

    if (httpClient != null) {
      // Calculate the checksums for remote files
      for (URI remoteFile : getRemoteFiles()) {
        if (remoteFile.getScheme().equals("jar")) {
          String checksum = calculateChecksumForJarURI(remoteFile);
          checksums.add(remoteFile, checksum);
        } else {
          // Download the file
          try (Response response = httpClient.newCall(new Request.Builder()
              .url(remoteFile.toURL())
              .get()
              .build())
              .execute()) {

            if (response.code() != 200) {
              throw new IOException("Failed to calculate checksum for " + remoteFile.toASCIIString() +
                  ", server returned " + response.code() + " (" + response.message() + ")");
            }

            // Calculate the checksum
            byte[] data = response.body().bytes();
            checksums.add(remoteFile, Util.md5Hex(data));
          }
        }
      }
    } else {
      getLogger().warn("Can't recalculate checksums for remote files because gradle is in offline mode");

      for (URI remoteFile : getRemoteFiles()) {
        if (!checksums.has(remoteFile)) {
          throw new IOException("Missing checksum for " + remoteFile.toASCIIString() +
              ", but can't recalculate because gradle is in offline modes");
        }
      }
    }

    // Save the checksum cache
    checksums.save(cacheFile);
  }

  /**
   * Calculates the checksum for a file with {@code jar:something} URI.
   * <p>
   * <b>Please note that this is a workaround and not something that should stay forever!</b>
   *
   * @param uri The URI to calculate the checksum for
   * @return The calculated checksum
   */
  private String calculateChecksumForJarURI(URI uri) {
    try {
      // Can't use the HTTP client, it does not support `jar:` URLs
      URLConnection connection = uri.toURL().openConnection();

      // Calculate the checksum by reading the stream
      String digest;
      try (InputStream stream = connection.getInputStream()) {
        digest = Util.md5Hex(Util.toByteArray(stream));
      }

      return digest;
    } catch (IOException e) {
      throw new FlintGradleException("Failed to calculate checksum for `jar:` URL", e);
    }
  }
}
