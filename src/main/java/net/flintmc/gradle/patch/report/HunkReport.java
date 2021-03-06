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

package net.flintmc.gradle.patch.report;

import com.cloudbees.diff.Hunk;
import net.flintmc.gradle.patch.state.PatchStatus;

public class HunkReport {

  private final PatchStatus patchStatus;
  private final Throwable failure;
  private final int index;
  private final int attempt;
  private final int hunkIdentifier;
  private final Hunk hunk;

  /**
   * Constructs a new {@link HunkReport} with the given parameters.
   *
   * @param patchStatus The status of a patch.
   * @param failure The reason why the patch process failed.
   * @param index The index of this hunk report.
   * @param attempt The attempts of this hunk report.
   * @param hunkIdentifier The identifier of this hunk report.
   * @param hunk The hunk for this hunk report.
   */
  private HunkReport(
      PatchStatus patchStatus,
      Throwable failure,
      int index,
      int attempt,
      int hunkIdentifier,
      Hunk hunk) {
    this.patchStatus = patchStatus;
    this.failure = failure;
    this.index = index;
    this.attempt = attempt;
    this.hunkIdentifier = hunkIdentifier;
    this.hunk = hunk;
  }

  /**
   * Creates a new {@link HunkReport} with the given parameters.
   *
   * @param patchStatus The status of a patch.
   * @param failure The reason why the patch process failed.
   * @param index The index of this hunk report.
   * @param attempt The attempts of this hunk report.
   * @param hunkIdentifier The identifier of this hunk report.
   * @return A new hunk report.
   */
  public static HunkReport create(
      PatchStatus patchStatus, Throwable failure, int index, int attempt, int hunkIdentifier) {
    return create(patchStatus, failure, index, attempt, hunkIdentifier, null);
  }

  /**
   * Creates a new {@link HunkReport} with the given parameters.
   *
   * @param patchStatus The status of a patch.
   * @param failure The reason why the patch process failed.
   * @param index The index of this hunk report.
   * @param attempt The attempts of this hunk report.
   * @param hunkIdentifier The identifier of this hunk report.
   * @param hunk The hunk for this report.
   * @return A new hunk report.
   */
  public static HunkReport create(
      PatchStatus patchStatus,
      Throwable failure,
      int index,
      int attempt,
      int hunkIdentifier,
      Hunk hunk) {
    return new HunkReport(patchStatus, failure, index, attempt, hunkIdentifier, hunk);
  }

  /**
   * Retrieves the status of the patch.
   *
   * @return The patch status.
   */
  public PatchStatus getPatchStatus() {
    return patchStatus;
  }

  /**
   * Retrieves the reason why the patch process failed.
   *
   * @return The reason why the patch process failed.
   */
  public Throwable getFailure() {
    return failure;
  }

  /**
   * Retrieves the index of this hunk report.
   *
   * @return The hunk report index.
   */
  public int getIndex() {
    return index;
  }

  /**
   * Retrieves the attempt of this hunk report.
   *
   * @return The hunk report attempt.
   */
  public int getAttempt() {
    return attempt;
  }

  /**
   * Retrieves the identifier of this hunk report.
   *
   * @return The hunk report's identifier.
   */
  public int getHunkIdentifier() {
    return hunkIdentifier;
  }

  /**
   * Retrieves the hunk of this report.
   *
   * @return The hunk of this report.
   */
  public Hunk getHunk() {
    return hunk;
  }

  /**
   * Whether the patch process has failed.
   *
   * @return {@code true} if the patch process has failed, otherwise {@code false}.
   */
  public boolean hasFailed() {
    return this.patchStatus == PatchStatus.FAILURE;
  }
}
