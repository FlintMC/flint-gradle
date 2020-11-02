package net.flintmc.gradle.manifest;

public class ManifestMavenDependency {

  private final String groupId;
  private final String name;
  private final String version;
  private final String url;

  public ManifestMavenDependency(String groupId, String name, String version, String url) {
    this.groupId = groupId;
    this.name = name;
    this.version = version;
    this.url = url;
  }

  public String getName() {
    return name;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getVersion() {
    return version;
  }

  public String getUrl() {
    return url;
  }
}
