package net.flintmc.gradle.manifest;

public class ManifestPackageDependency {

  private final String group;
  private final String name;
  private final String version;
  private final String namespace;

  public ManifestPackageDependency(String group, String name, String version, String namespace) {
    this.group = group;
    this.name = name;
    this.version = version;
    this.namespace = namespace;
  }

  public String getName() {
    return name;
  }

  public String getGroup() {
    return group;
  }

  public String getVersion() {
    return version;
  }

  public String getNamespace() {
    return namespace;
  }
}
