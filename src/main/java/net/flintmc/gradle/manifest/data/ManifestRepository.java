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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URI;

/**
 * Represents a repository which can be present in the cached manifest input.
 */
public class ManifestRepository implements Externalizable {
  private URI uri;
  private String credentialHeader;
  private String credentialContent;

  /**
   * Constructs a new {@link ManifestRepository}.
   *
   * @param uri               The URI of the repository
   * @param credentialHeader  The name of the header used for authentication (may be null)
   * @param credentialContent The value of the header used for authentication (may be null)
   */
  public ManifestRepository(URI uri, String credentialHeader, String credentialContent) {
    this.uri = uri;
    this.credentialHeader = credentialHeader;
    this.credentialContent = credentialContent;
  }

  /**
   * Constructs a new {@link ManifestRepository} with all values set to null, used for deserialization.
   */
  public ManifestRepository() {
  }

  /**
   * Retrieves the URI of this repository.
   *
   * @return The URI of this repository
   */
  public URI getURI() {
    return uri;
  }

  /**
   * Determines whether this repository requires authentication.
   *
   * @return {@code true} if this repository requires authentication, {@code false} otherwise
   */
  public boolean requiresAuthentication() {
    return credentialHeader != null;
  }

  /**
   * Retrieves the name of the header used for authentication.
   * <p>
   * Always {@code null} if {@link #requiresAuthentication()} is {@code false}, else never {@code null}.
   *
   * @return The name of the header used for authentication
   */
  public String getCredentialHeader() {
    return credentialHeader;
  }

  /**
   * Retrieves the value of the header used for authentication.
   * <p>
   * Always {@code null} if {@link #requiresAuthentication()} is {@code false}, else never {@code null}.
   *
   * @return The value of the header used for authentication
   */
  public String getCredentialContent() {
    return credentialContent;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(uri);
    out.writeObject(credentialHeader); // nullable, thus writeObject instead of writeUTF
    out.writeObject(credentialContent); // nullable, thus writeObject instead of writeUTF
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    uri = (URI) in.readObject();
    credentialHeader = (String) in.readObject(); // nullable, thus readObject instead of readUTF
    credentialContent = (String) in.readObject(); // nullable, thus readObject instead of readUTF
  }
}
