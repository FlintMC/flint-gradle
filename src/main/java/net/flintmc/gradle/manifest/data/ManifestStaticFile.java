package net.flintmc.gradle.manifest.data;

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
  private String path;
  private String md5sum;

  /**
   * Constructs a new {@link ManifestStaticFile}.
   *
   * @param uri    The URI where the file can be downloaded from
   * @param path   The path to put the file at
   * @param md5sum The MD5 checksum of the file
   */
  public ManifestStaticFile(URI uri, String path, String md5sum) {
    this.uri = uri;
    this.path = path;
    this.md5sum = md5sum;
  }

  /**
   * Constructs a new {@link ManifestStaticFile} with all values set to null, used for deserialization.
   */
  public ManifestStaticFile() {}

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(uri);
    out.writeUTF(path);
    out.writeUTF(md5sum);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    uri = (URI) in.readObject();
    path = in.readUTF();
    md5sum = in.readUTF();
  }
}
