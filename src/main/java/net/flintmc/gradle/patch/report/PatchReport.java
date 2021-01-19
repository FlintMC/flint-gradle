package net.flintmc.gradle.patch.report;

import java.util.List;
import net.flintmc.gradle.patch.state.PatchStatus;

public class PatchReport {

  private final String target;
  private final boolean binary;
  private final PatchStatus status;
  private final Throwable failure;
  private final List<HunkReport> hunkReports;

  /**
   * Constructs a new {@link PatchReport} with the given parameters.
   *
   * @param target The target on which the patch should be applied.
   * @param binary {@code true} if the patch is binary, otherwise {@code false}.
   * @param status The status of the patch.
   * @param failure The reason why the patch process failed.
   * @param hunkReports A collection with all hunk reports.
   */
  private PatchReport(
      String target,
      boolean binary,
      PatchStatus status,
      Throwable failure,
      List<HunkReport> hunkReports) {
    this.target = target;
    this.binary = binary;
    this.status = status;
    this.failure = failure;
    this.hunkReports = hunkReports;
  }

  /**
   * Creates a new {@link PatchReport} with the given parameters.
   *
   * @param target The target on which the patch should be applied.
   * @param binary {@code true} if the patch is binary, otherwise {@code false}.
   * @param status The status of the patch.
   * @param throwable The reason why the patch process failed.
   * @param hunkReports A collection with all hunk reports.
   * @return A created patch report.
   */
  public static PatchReport create(
      String target,
      boolean binary,
      PatchStatus status,
      Throwable throwable,
      List<HunkReport> hunkReports) {
    return new PatchReport(target, binary, status, throwable, hunkReports);
  }

  /**
   * Retrieves the target on which the patch should be applied.
   *
   * @return The target on which the patch should be applied.
   */
  public String getTarget() {
    return target;
  }

  /**
   * Whether the patch is binary.
   *
   * @return {@code true} if the patch is binary, otherwise {@code false}.
   */
  public boolean isBinary() {
    return binary;
  }

  /**
   * Retrieves the status of the patch.
   *
   * @return The patch status.
   */
  public PatchStatus getStatus() {
    return status;
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
   * Retrieves a collection with all hunk reports.
   *
   * @return A collection with all hunk reports.
   */
  public List<HunkReport> getHunkReports() {
    return hunkReports;
  }
}
