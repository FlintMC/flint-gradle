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
  public ManifestRepository() {}

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(uri);
    out.writeUTF(credentialHeader);
    out.writeUTF(credentialContent);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    uri = (URI) in.readObject();
    credentialHeader = in.readUTF();
    credentialContent = in.readUTF();
  }
}
