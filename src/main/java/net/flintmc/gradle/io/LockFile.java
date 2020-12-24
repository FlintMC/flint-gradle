/*
 * FlintMC
 * Copyright (C) 2020-2021 LabyMedia GmbH and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

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
