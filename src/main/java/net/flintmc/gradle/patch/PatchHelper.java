package net.flintmc.gradle.patch;

/** A helper object for patch files. */
public final class PatchHelper {

  /**
   * Checks if the specified {@code patch} is similar to the specified {@code target} and {@code
   * hunk}.
   *
   * @param patch The patch to be checked.
   * @param target The target to be checked.
   * @param hunk The hunk to be checked.
   * @return {@code true} if the specified {@code patch} is similar to the specified {@code target}
   *     and {@code hunk}, otherwise {@code false}.
   */
  public static boolean similar(PatchContextual patch, String target, String hunk) {
    if (patch.isC14nAccess()) {
      if (patch.isC14nWhitespace()) {
        target = target.replaceAll("[\t| ]+", " ");
        hunk = hunk.replaceAll("[\t| ]+", " ");
      }
      String[] targetSplit = target.split(" ");
      String[] hunkSplit = hunk.split(" ");

      int targetIndex = 0;
      int hunkIndex = 0;
      while (targetIndex < targetSplit.length && hunkIndex < hunkSplit.length) {
        boolean isTargetAccess = isAccess(targetSplit[targetIndex]);
        boolean isHunkAccess = isAccess(hunkSplit[hunkIndex]);
        if (isTargetAccess || isHunkAccess) {

          if (isTargetAccess) {
            targetIndex++;
          }
          if (isHunkAccess) {
            hunkIndex++;
          }
          continue;
        }
        String hunkPart = hunkSplit[hunkIndex];
        String targetPart = targetSplit[targetIndex];
        boolean labels = isLabel(targetPart) && isLabel(hunkPart);
        if (!labels && !targetPart.equals(hunkPart)) {
          return false;
        }
        hunkIndex++;
        targetIndex++;
      }
      return hunkSplit.length == hunkIndex && targetSplit.length == targetIndex;
    }
    if (patch.isC14nWhitespace()) {
      return target.replaceAll("[\t| ]+", " ").equals(hunk.replaceAll("[\t| ]+", " "));
    } else {
      return target.equals(hunk);
    }
  }

  /**
   * Checks if the given data is a valid access.
   *
   * @param data The data to check.
   * @return {@code true} if the given data is a valid access, otherwise {@code false}.
   */
  private static boolean isAccess(String data) {
    return data.equalsIgnoreCase("public")
        || data.equalsIgnoreCase("private")
        || data.equalsIgnoreCase("protected")
        || data.equalsIgnoreCase("final");
  }

  /**
   * Checks whether the specified data starts with "label".
   *
   * @param data The data to check.
   * @return {@code true} if the specified data starts with "label", otherwise {@code false}.
   */
  private static boolean isLabel(String data) {
    return data.startsWith("label");
  }
}
