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

package net.flintmc.gradle.environment.mcp;

import net.flintmc.gradle.environment.SourceJarAction;

import java.util.Arrays;
import java.util.List;

/**
 * Utility source action stripping forge additions causing the source to fail to recompile.
 */
public class ForgeAdditionStripper implements SourceJarAction {
  private static final List<String> ANNOTATION_IMPORTS = Arrays.asList(
      "javax.annotation.Nullable",
      "javax.annotation.Nonnull",
      "javax.annotation.concurrent.Immutable",
      "net.minecraftforge.api.distmarker.Dist", // Annotation value only
      "net.minecraftforge.api.distmarker.OnlyIn"
  );

  private static final List<String> ANNOTATIONS = Arrays.asList(
      "Nullable",
      "Nonnull",
      "Immutable",
      "OnlyIn"
  );

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(StringBuffer snippet) {
    for (String annotation : ANNOTATION_IMPORTS) {
      // Search for import lines
      String annotationImport = "import " + annotation + ";";
      int importIndex = snippet.indexOf(annotationImport);

      if (importIndex != -1) {
        // Found an import line, delete it (assume the line is ending with a \n)
        snippet.delete(importIndex, importIndex + annotationImport.length() + 1);
      }
    }

    // Strip all known annotations
    for (String annotationName : ANNOTATIONS) {
      // Construct the name containing the @
      String annotation = "@" + annotationName;

      // Get the indices of the annotation
      int indexOfAnnotation;
      while ((indexOfAnnotation = snippet.indexOf(annotation)) != -1) {
        int annotationEndIndex = indexOfAnnotation + annotation.length();

        // Found an annotation
        if (snippet.length() > annotationEndIndex) {
          // If there is something after the annotation, inspect it
          int parenCount = 0;

          do {
            char annotationTrail = snippet.charAt(annotationEndIndex);

            // If the trail is an open paren, increase the count
            // (Note: This does not take strings in annotations in count, for the ones specified
            // on this stripper, this is ok)
            if (annotationTrail == '(') {
              parenCount++;
            } else if (annotationTrail == ')') {
              // Closing paren, decrease the count
              parenCount--;
            }

            if (parenCount != 0) {
              // Increase the length of the annotation
              annotationEndIndex++;
            }

            // Repeat until we have captured the entire annotation
          } while (parenCount != 0);
        }

        // Remove the annotation
        snippet.delete(indexOfAnnotation, annotationEndIndex + 1);
      }
    }
  }
}
