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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Input stream which when closed delegates the closing to its delegate stream and other closeables.
 */
public class MultiResourceInputStream extends InputStream {
  private final Set<AutoCloseable> closeables;
  private final InputStream delegate;

  /**
   * Constructs a new {@link MultiResourceInputStream} a delegate and other closeables.
   *
   * @param delegate   The delegate to delegate stream operations to
   * @param closeables The other closeables to close when the delegate has been closed
   */
  public MultiResourceInputStream(InputStream delegate, AutoCloseable... closeables) {
    this.closeables = new LinkedHashSet<>(Arrays.asList(closeables));
    this.delegate = delegate;
  }

  @Override
  public void close() throws IOException {
    IOException ex = null;

    try {
      delegate.close();
    } catch(IOException e) {
      ex = e;
    }

    // Try to close as many closeables no matter what
    for(AutoCloseable closeable : closeables) {
      try {
        closeable.close();
      } catch(Exception e) {
        if(e instanceof IOException && ex == null) {
          // First exception, and its an I/O one, use it directly
          ex = (IOException) e;
        } else {
          if(ex == null) {
            // First exception, but not an I/O exception, wrap it
            ex = new IOException("Exception while closing MultiResourceInputStream", e);
          } else {
            // Earlier an exception occurred already, suppress this one
            ex.addSuppressed(e);
          }
        }
      }
    }

    if(ex != null) {
      // At least on exception was thrown
      throw ex;
    }
  }

  @Override
  public int read() throws IOException {
    return delegate.read();
  }

  @Override
  public int read(@NotNull byte[] b) throws IOException {
    return delegate.read(b);
  }

  @Override
  public int read(@NotNull byte[] b, int off, int len) throws IOException {
    return delegate.read(b, off, len);
  }

  @Override
  public long skip(long n) throws IOException {
    return delegate.skip(n);
  }

  @Override
  public int available() throws IOException {
    return delegate.available();
  }

  @Override
  public synchronized void mark(int readlimit) {
    delegate.mark(readlimit);
  }

  @Override
  public synchronized void reset() throws IOException {
    delegate.reset();
  }

  @Override
  public boolean markSupported() {
    return delegate.markSupported();
  }
}
