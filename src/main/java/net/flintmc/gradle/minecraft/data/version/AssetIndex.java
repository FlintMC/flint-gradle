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

import java.net.URI;
import java.util.Objects;

public class AssetIndex {
  private String id;
  private String sha1;
  private long size;
  private long totalSize;
  private URI url;

  public String getId() {
    return id;
  }

  public String getSha1() {
    return sha1;
  }

  public long getSize() {
    return size;
  }

  public long getTotalSize() {
    return totalSize;
  }

  public URI getUrl() {
    return url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AssetIndex that = (AssetIndex) o;
    return size == that.size &&
        totalSize == that.totalSize &&
        Objects.equals(id, that.id) &&
        Objects.equals(sha1, that.sha1) &&
        Objects.equals(url, that.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, sha1, size, totalSize, url);
  }
}
