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

package net.flintmc.gradle.patch.state;

/** Represents an enumeration with all available patch statuses. */
public enum PatchStatus {
  /** When the patch successfully patches the file. */
  PATCHED(true),
  /** When the patch is missing. */
  MISSING(false),
  /** When the patch has failed. */
  FAILURE(false),
  /** When the patch is skipped. */
  SKIPPED(true),
  /** When the patch was attempted. */
  TRIED(true);

  private final boolean success;

  PatchStatus(boolean success) {
    this.success = success;
  }

  /**
   * Whether the patch status was successful.
   *
   * @return {@code true} if the patch status was successful, otherwise {@code false}.
   */
  public boolean isSuccess() {
    return this.success;
  }
}
