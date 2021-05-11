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

package net.flintmc.gradle.manifest.data;

import net.flintmc.installer.util.OperatingSystem;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URI;

/**
 * Represents a static file entry which can be present in the cached manifest input.
 */
public class ManifestStaticFile implements Externalizable {
  private URI uri;
  private OperatingSystem operatingSystem;
  private String path;

  /**
   * Constructs a new {@link ManifestStaticFile}.
   *  @param uri  The URI where the file can be downloaded from
   * @param operatingSystem The operating system where the file needs to be downloaded
   * @param path The path to put the file at
   */
  public ManifestStaticFile(URI uri, OperatingSystem operatingSystem, String path) {
    this.uri = uri;
    this.operatingSystem = operatingSystem;
    this.path = path;
  }

  /**
   * Constructs a new {@link ManifestStaticFile} with all values set to null, used for deserialization.
   */
  public ManifestStaticFile() {
  }

  /**
   * Retrieves the URI this file will be downloaded from.
   *
   * @return The URI this file will be downloaded from
   */
  public URI getURI() {
    return uri;
  }

  public OperatingSystem getOperatingSystem() {
    return this.operatingSystem;
  }

  /**
   * Retrieves the path this file will be downloaded to
   *
   * @return The path this file will be downloaded to
   */
  public String getPath() {
    return path;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(uri);
    out.writeByte(this.operatingSystem == null ? -1 : this.operatingSystem.ordinal());
    out.writeUTF(path);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    uri = (URI) in.readObject();
    int osIndex = in.readByte();
    this.operatingSystem = osIndex == -1 ? null : OperatingSystem.values()[osIndex];
    path = in.readUTF();
  }
}
