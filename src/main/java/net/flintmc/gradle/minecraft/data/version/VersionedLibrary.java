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

package net.flintmc.gradle.minecraft.data.version;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.flintmc.gradle.maven.pom.MavenArtifact;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class VersionedLibrary {
  private MavenArtifact name;
  private VersionedLibraryDownloads downloads;
  @JsonProperty("extract")
  private Map<String, List<String>> extractRules;
  private Map<String, String> natives;
  private List<VersionedRule> rules;

  public MavenArtifact getName() {
    return name;
  }

  public VersionedLibraryDownloads getDownloads() {
    return downloads;
  }

  public Map<String, List<String>> getExtractRules() {
    return extractRules;
  }

  public Map<String, String> getNatives() {
    return natives;
  }

  public List<VersionedRule> getRules() {
    return rules;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    VersionedLibrary that = (VersionedLibrary) o;
    return Objects.equals(name, that.name) &&
        Objects.equals(downloads, that.downloads) &&
        Objects.equals(extractRules, that.extractRules) &&
        Objects.equals(natives, that.natives) &&
        Objects.equals(rules, that.rules);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, downloads, extractRules, natives, rules);
  }
}
