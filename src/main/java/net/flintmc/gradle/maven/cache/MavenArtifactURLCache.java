package net.flintmc.gradle.maven.cache;

import net.flintmc.gradle.FlintGradleException;
import net.flintmc.gradle.io.FileLock;
import net.flintmc.gradle.maven.MavenArtifactDownloader;
import net.flintmc.gradle.maven.ReadableMavenRepository;
import net.flintmc.gradle.maven.RemoteMavenRepository;
import net.flintmc.gradle.maven.pom.MavenArtifact;
import net.flintmc.gradle.util.Pair;
import net.flintmc.gradle.util.Util;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** Cache for maven artifacts and their corresponding URL's. */
public class MavenArtifactURLCache {
  private static final Logger LOGGER = Logging.getLogger(MavenArtifactURLCache.class);

  private final Map<MavenArtifact, Collection<URI>> negativeCache;
  private final Path artifactURLCacheFile;
  private final boolean offline;
  private final Map<MavenArtifact, Map<URI, URI>> cache;
  private final Lock processLocalLock;

  private long lastValidation;

  /**
   * Constructs a new {@link MavenArtifactURLCache}.
   *
   * @param artifactURLCacheFile The file to store cached data in
   * @param offline Whether the current build is an offline build
   */
  public MavenArtifactURLCache(Path artifactURLCacheFile, boolean offline) {
    this.negativeCache = new HashMap<>();
    this.artifactURLCacheFile = artifactURLCacheFile;
    this.offline = offline;
    this.cache = new HashMap<>();

    this.processLocalLock = new ReentrantLock();
  }

  /**
   * Performs initial setup of the cache.
   *
   * @throws IOException If an I/O error occurs
   */
  public void setup() throws IOException {
    boolean wasMissing = !Files.isRegularFile(artifactURLCacheFile);

    if (wasMissing) {
      // Create the parent directories and the file itself so it can be locked
      Files.createDirectories(artifactURLCacheFile.getParent());
      Files.createFile(artifactURLCacheFile);
    }

    // Lock the file
    lock(
        (cacheChannel) -> {
          if (wasMissing) {
            // The file does exist now, but is empty, so fill it with dummy data
            // so that later loads succeed
            save(cacheChannel);
          } else {
            // The file did exist already, load it now
            load(cacheChannel);
          }
          return null;
        });
  }

  /**
   * Resolves all artifact URL's from the cache or online if possible.
   *
   * @param artifacts The artifacts to resolve
   * @param remoteRepositories The repositories to use for resolving
   * @param resolveFullURI If {@code true}, the full artifact URI will be returned, if {@code
   *     false}, the maven repository base URI will be returned
   * @return The artifacts and their resolved URL's
   * @throws IOException If an I/O error occurs
   */
  public Map<MavenArtifact, URI> resolve(
      Collection<MavenArtifact> artifacts,
      Collection<RemoteMavenRepository> remoteRepositories,
      boolean resolveFullURI)
      throws IOException {
    Map<MavenArtifact, URI> out = new HashMap<>();
    Collection<MavenArtifact> missing =
        resolveInternal(out, artifacts, remoteRepositories, resolveFullURI, true);

    if (missing.isEmpty()) {
      return out;
    } else if (offline) {
      LOGGER.error(
          "Failed to resolve {} artifact URL's because the plugin is in offline mode.",
          missing.size());
      LOGGER.error("Please connect to the internet and try again!");
    } else {
      LOGGER.error("Failed to resolve {} artifact URL's.", missing.size());
      LOGGER.error("The artifacts that failed to resolve are:");
      for (MavenArtifact artifact : missing) {
        LOGGER.error("- {}", artifact);
      }
      LOGGER.error("Searched the following repositories:");
      for (RemoteMavenRepository remoteRepository : remoteRepositories) {
        LOGGER.error("- {}", remoteRepository.getBaseURI());
      }
    }

    throw new FlintGradleException("Failed to resolve " + missing.size() + " artifact URL's");
  }

  /**
   * Resolves all artifact URL's from the cache or online if possible.
   *
   * @param out The map to store resolved artifacts and their URL's into
   * @param artifacts The artifacts to resolve the URL's for
   * @param remoteRepositories The repositories to consider while searching
   * @param resolveFullURI If {@code true}, the full artifact URI will be returned, if {@code
   *     false}, the maven repository base URI will be returned
   * @param autoResolveMissing If {@code true}, this method will try to resolve missing artifacts
   *     online
   * @return A collection of artifacts which could not be resolved
   * @throws IOException If an I/O error occurs
   */
  private Collection<MavenArtifact> resolveInternal(
      Map<MavenArtifact, URI> out,
      Collection<MavenArtifact> artifacts,
      Collection<RemoteMavenRepository> remoteRepositories,
      boolean resolveFullURI,
      boolean autoResolveMissing)
      throws IOException {
    Collection<MavenArtifact> missing = new HashSet<>();

    for (MavenArtifact artifact : artifacts) {
      Map<URI, URI> known = cache.get(artifact);
      if (known == null) {
        // Artifact has not been cached
        missing.add(artifact);
        continue;
      }

      URI foundURI = null;

      // Find the artifact based on the available repositories
      for (RemoteMavenRepository remoteRepository : remoteRepositories) {
        URI baseURI = remoteRepository.getBaseURI();
        URI artifactURI = known.get(baseURI);

        if (artifactURI != null) {
          // Found the artifact
          foundURI = resolveFullURI ? artifactURI : baseURI;
          break;
        }
      }

      if (foundURI != null) {
        // Artifact has been found
        out.put(artifact, foundURI);
      } else {
        // Artifact has not been found in the cache
        missing.add(artifact);
      }
    }

    if (autoResolveMissing && !offline) {
      // Missing artifacts, try to find them online
      return resolveMissing(out, artifacts, remoteRepositories, resolveFullURI);
    } else {
      return missing;
    }
  }

  /**
   * Resolves missing artifacts from remote repositories.
   *
   * @param out The map to store resolved artifacts into
   * @param artifacts The artifacts to resolve
   * @param remoteRepositories The repositories to resolve the artifacts from
   * @param resolveFullURI If {@code true}, the full artifact URI will be returned, if {@code
   *     false}, the maven repository base URI will be returned
   * @return All artifacts which could not be resolved
   * @throws IOException If an I/O error occurs
   */
  private Collection<MavenArtifact> resolveMissing(
      Map<MavenArtifact, URI> out,
      Collection<MavenArtifact> artifacts,
      Collection<RemoteMavenRepository> remoteRepositories,
      boolean resolveFullURI)
      throws IOException {
    return lock(
        (cacheChannel) -> {
          // Reload the cache file, we now have a lock on it
          load(cacheChannel);

          // Now that we have reloaded the cache file, try to resolve again
          Collection<MavenArtifact> stillMissing =
              resolveInternal(out, artifacts, remoteRepositories, resolveFullURI, false);
          if (stillMissing.isEmpty()) {
            // All dependencies resolved after reload, nothing left to do
            return Collections.emptySet();
          }

          Set<MavenArtifact> unresolvable = new HashSet<>();

          // Resolve every single missing artifact
          for (MavenArtifact missing : stillMissing) {
            // Still missing some dependencies, try resolving them now
            MavenArtifactDownloader downloader = new MavenArtifactDownloader();
            Collection<URI> negativeCache = getOrCreateNegativeCacheEntry(missing);

            for (RemoteMavenRepository remoteRepository : remoteRepositories) {
              if (!negativeCache.contains(remoteRepository.getBaseURI())) {
                // The repository has not been checked for the artifact yet
                downloader.addSource(remoteRepository);
              }
            }

            Pair<ReadableMavenRepository, URI> result = downloader.findArtifactURI(missing);

            if (result != null) {
              // Resolved the artifact
              RemoteMavenRepository remoteMavenRepository =
                  (RemoteMavenRepository) result.getFirst();
              getOrCreateCacheEntry(missing)
                  .put(remoteMavenRepository.getBaseURI(), result.getSecond());
              out.put(
                  missing,
                  resolveFullURI ? result.getSecond() : remoteMavenRepository.getBaseURI());
            } else {
              // Failed to resolve the artifact
              unresolvable.add(missing);

              // Add the URL's to the negative cache as the artifact could not be resolved in them
              for (RemoteMavenRepository remoteRepository : remoteRepositories) {
                negativeCache.add(remoteRepository.getBaseURI());
              }
            }
          }

          // Save the cache after resolving
          save(cacheChannel);

          return unresolvable;
        });
  }

  /**
   * Retrieves a cache entry for the given artifact or creates it if necessary.
   *
   * @param artifact The artifact to retrieve the cache entry for
   * @return The cache entry
   */
  private Map<URI, URI> getOrCreateCacheEntry(MavenArtifact artifact) {
    return cache.computeIfAbsent(artifact, k -> new HashMap<>());
  }

  /**
   * Retrieves a negative cache entry for the given artifact or creates it if necessary.
   *
   * @param artifact The artifact to retrieve the negative cache entry for
   * @return The negative cache entry
   */
  private Collection<URI> getOrCreateNegativeCacheEntry(MavenArtifact artifact) {
    return negativeCache.computeIfAbsent(artifact, k -> new HashSet<>());
  }

  /**
   * Opens and locks the cache file.
   *
   * <p>This method implements a whole try-with-resources and beyond error handling, the callback is
   * used to deduplicate code as the error handling would else have to appear multiple times in this
   * file.
   *
   * @param callback The callback to call for when the file has been locked
   * @return The return value of the callback
   * @throws IOException If an I/O error occurs
   */
  private <T> T lock(FileLockedCallback<T> callback) throws IOException {
    try {
      processLocalLock.lock();
      FileChannel cacheChannel = FileChannel.open(artifactURLCacheFile, StandardOpenOption.READ, StandardOpenOption.WRITE);

      FileLock lock;
      try {
        lock = FileLock.tryAcquire(cacheChannel);
        if(lock == null) {
          LOGGER.warn("Artifact URL cache is currently locked, are your running multiple builds at once?");
          LOGGER.warn("Trying to acquire lock (this will block until the lock has been released by the other build!)");
          lock = FileLock.acquire(cacheChannel);
        }
      } catch(IOException e) {
        try {
          cacheChannel.close();
        } catch(IOException nested) {
          e.addSuppressed(nested);
        }
        throw e;
      }

      Throwable originalThrowable = null;
      try {
        // Try to run the callback
        return callback.execute(cacheChannel);
      } catch(Throwable t) {
        // Callback failed, catch the error so it can be used in the finally block
        originalThrowable = t;

        // Re-throw now
        throw t;
      } finally {
        IOException inner = null;

        try {
          // Try to release the lock
          lock.release();
        } catch(IOException e) {
          if(originalThrowable != null) {
            // Releasing the lock failed, but so did the callback, suppress the unlock failure
            originalThrowable.addSuppressed(e);
          } else {
            // The callback succeeded, but unlocking failed
            inner = e;
          }
        }

        try {
          // Try to close the channel
          cacheChannel.close();
        } catch(IOException e) {
          if(originalThrowable != null) {
            // Closing the channel failed, but so did the callback, suppress the closing failure
            originalThrowable.addSuppressed(e);
          } else if(inner != null) {
            // The callback succeeded, but unlocking failed, suppress the closing failure
            inner.addSuppressed(e);
          } else {
            // The callback succeeded and so did unlocking, catch this error
            inner = e;
          }
        }

        if(inner != null) {
          // Re-throw any possible resource failure
          //noinspection ThrowFromFinallyBlock
          throw inner;
        }
      }
    } finally {
      processLocalLock.unlock();
    }
  }

  /**
   * Loads the cache file from disk.
   *
   * @param channel The channel to read from
   * @throws IOException If an I/O error occurs
   */
  private void load(FileChannel channel) throws IOException {
    cache.clear();

    // This is not a resource leak, the channel will be closed later
    ObjectInputStream in = new ObjectInputStream(Channels.newInputStream(channel));
    try {
      lastValidation = in.readLong();

      int entriesCount = in.readInt();
      for (int i = 0; i < entriesCount; i++) {
        // Read in one artifact
        String groupId = in.readUTF();
        String artifactId = in.readUTF();
        String version = in.readUTF();
        String classifier =
            (String) in.readObject(); // nullable, thus readObject instead of readUTF
        String type = (String) in.readObject(); // nullable, thus readObject instead of readUTF

        MavenArtifact artifact = new MavenArtifact(groupId, artifactId, version, classifier, type);

        // This is serializable, so it can be read in automatically
        Map<URI, URI> knownURIs = Util.forceCast(in.readObject());

        cache.put(artifact, knownURIs);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(
          "UNREACHABLE: ClassNotFoundException while reading ObjectInputStream with only standard library classes",
          e);
    }
  }

  /**
   * Saves the cache file to disk.
   *
   * @param channel The channel to write to
   * @throws IOException If an I/O error occurs
   */
  private void save(FileChannel channel) throws IOException {
    channel.truncate(0);

    // This is not a resource leak, the channel will be closed later
    ObjectOutputStream out = new ObjectOutputStream(Channels.newOutputStream(channel));

    // Write out header
    out.writeLong(lastValidation);
    out.writeInt(cache.size());

    for (Map.Entry<MavenArtifact, Map<URI, URI>> cacheEntry : cache.entrySet()) {
      MavenArtifact artifact = cacheEntry.getKey();

      // Write out the cached artifact
      out.writeUTF(artifact.getGroupId());
      out.writeUTF(artifact.getArtifactId());
      out.writeUTF(artifact.getVersion());
      out.writeObject(artifact.getClassifier()); // nullable, thus writeObject instead of writeUTF
      out.writeObject(artifact.getType());

      // This is serializable, so it can be written automatically
      out.writeObject(cacheEntry.getValue());
    }

    out.flush();
  }

  /**
   * Callback for when the cache file has been locked.
   *
   * @param <T> The return type of the callback
   */
  private interface FileLockedCallback<T> {
    /**
     * Called when the cache file has been locked.
     *
     * @param channel The channel of the locked file
     * @return The return value of the callback
     * @throws IOException If the callback throws an {@link IOException}
     */
    T execute(FileChannel channel) throws IOException;
  }
}
