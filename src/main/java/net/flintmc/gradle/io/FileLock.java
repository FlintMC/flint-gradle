package net.flintmc.gradle.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple file lock.
 */
public class FileLock implements AutoCloseable {
  /**
   * Locks a file stream blocking until the lock is acquired.
   *
   * @param stream The stream to lock
   * @return The file lock
   * @throws IOException                  If locking fails
   * @throws OverlappingFileLockException If the file is already locked
   */
  public static FileLock acquire(FileInputStream stream) throws IOException, OverlappingFileLockException {
    return acquire(stream.getChannel());
  }

  /**
   * Locks a file channel blocking until the lock is acquired.
   *
   * @param channel The file channel to lock
   * @return The file lock
   * @throws IOException                  If locking fails
   * @throws OverlappingFileLockException If the file is already locked
   */
  public static FileLock acquire(FileChannel channel) throws IOException, OverlappingFileLockException {
    return new FileLock(channel.lock());
  }

  /**
   * Tries to lock a file stream.
   *
   * @param stream The stream to lock
   * @return The file lock, or {@code null}, if the lock is currently acquired by another process
   * @throws IOException                  If an I/O error occurs
   * @throws OverlappingFileLockException If this process already acquired the lock or is already trying to
   */
  public static FileLock tryAcquire(FileInputStream stream) throws IOException, OverlappingFileLockException {
    return tryAcquire(stream.getChannel());
  }

  /**
   * Tries to lock a file channel.
   *
   * @param channel The channel to lock
   * @return The file lock, or {@code null}, if the lock is currently acquired by another process
   * @throws IOException                  If an I/O error occurs
   * @throws OverlappingFileLockException If this process already acquired the lock or is already trying to
   */
  public static FileLock tryAcquire(FileChannel channel) throws IOException, OverlappingFileLockException {
    java.nio.channels.FileLock lock = channel.tryLock();

    if(lock == null) {
      return null;
    }

    return new FileLock(lock);
  }

  private final FileChannel internalChannel;
  private final Path internalLockPath;
  private final java.nio.channels.FileLock internal;

  /**
   * Constructs a new {@link FileLock} from an existing lock wrapping a file channel representing a lock file.
   *
   * @param internalChannel  The channel wrapping the lock file
   * @param internalLockPath The path to delete when releasing the lock
   * @param internal         The file lock acquire on the channel
   */
  FileLock(FileChannel internalChannel, Path internalLockPath, java.nio.channels.FileLock internal) {
    this.internalChannel = internalChannel;
    this.internalLockPath = internalLockPath;
    this.internal = internal;
  }

  /**
   * Constructs a new {@link FileLock} from an existing lock.
   *
   * @param internal The existing lock to wrap
   */
  public FileLock(java.nio.channels.FileLock internal) {
    this.internalChannel = null;
    this.internalLockPath = null;
    this.internal = internal;
  }

  /**
   * Releases the lock.
   *
   * @throws IOException If an I/O error occurs
   */
  public void release() throws IOException {
    internal.release();

    if(internalChannel != null) {
      internalChannel.close();
    }

    if(internalLockPath != null) {
      try {
        Files.deleteIfExists(internalLockPath);
      } catch(IOException ignored) {
        // Does not really matter, maybe another process has acquired the lock
      }
    }
  }

  @Override
  public void close() throws IOException {
    release();
  }
}
