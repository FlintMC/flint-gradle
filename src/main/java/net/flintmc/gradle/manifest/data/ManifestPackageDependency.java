package net.flintmc.gradle.manifest.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Represents a package dependency which can be present in the cached manifest input.
 */
public class ManifestPackageDependency implements Externalizable {
  private String name;
  private String version;
  private String channel;

  /**
   * Constructs a new {@link ManifestPackageDependency}.
   *
   * @param name    The name of the package this dependency refers to
   * @param version The version specification of the package this dependency refers to
   * @param channel The channel of the package this dependency refers to
   */
  public ManifestPackageDependency(String name, String version, String channel) {
    this.name = name;
    this.version = version;
    this.channel = channel;
  }

  /**
   * Constructs a new {@link ManifestPackageDependency} with all values set to null, used for deserialization.
   */
  public ManifestPackageDependency() {
  }

  /**
   * Retrieves the name of the package this dependency refers to.
   *
   * @return The name of the package this dependency refers to
   */
  public String getName() {
    return name;
  }

  /**
   * Retrieves the version specification of the package this dependency refers to.
   *
   * @return The version specification of the package this dependency refers to
   */
  public String getVersion() {
    return version;
  }

  /**
   * Retrieves the channel of the package this dependency refers to.
   *
   * @return The channel of the package this dependency refers to
   */
  public String getChannel() {
    return channel;
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeUTF(name);
    out.writeUTF(version);
    out.writeUTF(channel);
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
    name = in.readUTF();
    version = in.readUTF();
    channel = in.readUTF();
  }
}
