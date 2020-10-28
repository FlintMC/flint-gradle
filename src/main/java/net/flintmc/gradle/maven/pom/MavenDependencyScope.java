package net.flintmc.gradle.maven.pom;

public enum MavenDependencyScope {
  COMPILE("compile"),
  RUNTIME("runtime"),
  PROVIDED("provided"),
  SYSTEM("system"),
  TEST("test"),
  IMPORT("import");

  private final String mavenName;

  MavenDependencyScope(String mavenName) {
    this.mavenName = mavenName;
  }

  public String getMavenName() {
    return mavenName;
  }
}
