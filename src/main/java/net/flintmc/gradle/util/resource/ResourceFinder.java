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

package net.flintmc.gradle.util.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Utility for finding a specific resource from a classpath.
 *
 * @see ResourceLoader#findAll(String)
 */
public class ResourceFinder {
  private final String resourceName;
  private final Iterator<File> fileIterator;

  /**
   * Constructs a resource finder for the specified classpath
   *
   * @param resourceName The name of the resource to find
   * @param files        The files to treat as a classpath
   */
  ResourceFinder(String resourceName, Collection<File> files) {
    this.resourceName = resourceName;
    this.fileIterator = files.iterator();
  }

  /**
   * Opens an input stream to the next found resource, if any,
   *
   * @return The next found resource, or {@code null}, if none
   * @throws IOException If an I/O error occurs
   */
  public InputStream streamNext() throws IOException {
    while(fileIterator.hasNext()) {
      File file = fileIterator.next();

      if(guessIfZip(file)) {
        InputStream zipStream = getZipResourceStream(file);

        if(zipStream != null) {
          return zipStream;
        }
      }

      if(file.isDirectory()) {
        InputStream fileStream = getDirectoryResourceStream(file);

        if(fileStream != null) {
          return fileStream;
        }
      }
    }

    return null;
  }

  /**
   * Tries to guess if the file is a ZIP file.
   *
   * @param file The file to guess based on
   * @return {@code true} if the file is probably a ZIP file, {@code false} otherwise
   */
  private boolean guessIfZip(File file) {
    String name = file.getName();

    return name.endsWith(".zip") ||
        name.endsWith(".jar") ||
        name.endsWith(".ear") ||
        name.endsWith(".sar") ||
        name.endsWith(".war");
  }

  /**
   * Attempts to open the given file as a ZIP file and get a stream for the searched resource name.
   *
   * @param file The file to open
   * @return The found stream, or {@code null}, if the resource could not be found
   * @throws IOException If an I/O error occurs
   */
  private InputStream getZipResourceStream(File file) throws IOException {
    ZipFile zip = new ZipFile(file);

    try {
      ZipEntry entry = zip.getEntry(resourceName);

      if(entry != null && !entry.isDirectory()) {
        // We need to keep the ZIP open until the resource has been read, so
        // wrap it in a multi resource stream which will close both
        return new MultiResourceInputStream(zip.getInputStream(entry), zip);
      }
    } catch(IOException e) {
      try {
        zip.close();
      } catch(IOException second) {
        e.addSuppressed(second);
      }

      throw e;
    }

    return null;
  }

  /**
   * Attempts to open a file in a directory searching the directory for a child with the resource name.
   *
   * @param directory The directory to search for the resource
   * @return The found resource stream, {@code null}, if the resource could not be found
   * @throws IOException If an I/O error occurs
   */
  private InputStream getDirectoryResourceStream(File directory) throws IOException {
    File resource = new File(directory, resourceName);

    if(resource.exists() && resource.isFile()) {
      return new FileInputStream(resource);
    }

    return null;
  }
}
