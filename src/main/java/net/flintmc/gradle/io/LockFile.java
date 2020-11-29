package net.flintmc.gradle.io;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Wrapper for locks that are based on files but represent more a lock other than on the file itself.
 */
public class LockFile {
  private final Path lockFilePath;

  /**
   * Constructs a new {@link LockFile} for the given path, but does not acquire it.
   *
   * @param lockFilePath The path of the lock file
   */
  public LockFile(Path lockFilePath) {
    this.lockFilePath = lockFilePath;
  }

  /**
   * Acquires the file lock blocking until it can be acquired by this process.
   *
   * @return The acquired lock
   * @throws IOException                  If an I/O error occurs
   * @throws OverlappingFileLockException If this process already acquired the lock or is already trying to
   */
  public FileLock acquire() throws IOException, OverlappingFileLockException {
    if(!Files.isDirectory(lockFilePath.getParent())) {
      Files.createDirectories(lockFilePath.getParent());
    }

    FileChannel channel = FileChannel.open(lockFilePath);
    return new FileLock(channel, lockFilePath, channel.lock());
  }

  /**
   * Tries to acquire the file lock.
   *
   * @return The acquire lock, or {@code null}, if another process is currently locking the file
   * @throws IOException                  If an I/O error occurs
   * @throws OverlappingFileLockException If this process already acquired the lock or is already trying to
   */
  public FileLock tryAcquire() throws IOException, OverlappingFileLockException {
    if(!Files.isDirectory(lockFilePath.getParent())) {
      Files.createDirectories(lockFilePath.getParent());
    }

    FileChannel channel = FileChannel.open(lockFilePath);
    java.nio.channels.FileLock lock = channel.tryLock();

    if(lock == null) {
      return null;
    }

    return new FileLock(channel, lockFilePath, lock);
  }
}
