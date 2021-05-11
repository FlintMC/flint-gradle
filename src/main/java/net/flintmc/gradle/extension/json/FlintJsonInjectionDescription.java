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

package net.flintmc.gradle.extension.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.flintmc.installer.impl.repository.models.install.data.json.JsonInjectionOverrideType;
import net.flintmc.installer.install.json.JsonInjectionType;
import org.gradle.api.Named;
import org.gradle.api.Project;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entry in the container of the json injections block.
 *
 * @see FlintJsonInjectionsExtension
 */
public class FlintJsonInjectionDescription implements Named {
  private final Project project;
  private final String name;

  private JsonElement injection;
  private JsonInjectionType type = JsonInjectionType.MODIFY_ARRAY;
  private JsonInjectionOverrideType overrideType = JsonInjectionOverrideType.ALWAYS_ADD;
  private String[] pathEntries = new String[0];
  private String path;
  private String injectKey;
  private boolean prettyPrint;

  /**
   * Constructs a new {@link FlintJsonInjectionDescription}.
   *
   * @param project The project this description belongs to
   * @param name    The name of the file
   */
  public FlintJsonInjectionDescription(Project project, String name) {
    this.project = project;
    this.name = name;
  }

  /**
   * Parses json from the given source and sets it as the json to be injected into the target.
   *
   * @param source The source to read the json from
   */
  public void from(Object source) throws IOException {
    try (InputStream inputStream = new FileInputStream(project.file(source));
         Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      this.injection = JsonParser.parseReader(reader);
    }
  }

  /**
   * Sets the json that should be injected into the target.
   *
   * @param json The non-null json to be injected
   */
  public void inject(String json) {
    this.injection = JsonParser.parseString(json);
  }

  /**
   * Sets whether the generated json should be printed just in one line or with line breaks.
   *
   * @param prettyPrint {@code true} if it should be printed with line breaks, {@code false} otherwise
   */
  public void prettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

  /**
   * Sets the path in the json where the content should be injected.
   * For example, to inject something into the following object:
   * <pre>
   *   {
   *     "key1": {
   *       "key2": {
   *         // INJECT HERE
   *       }
   *     }
   *   }
   * </pre>
   * The entries would be "key1" and "key2".
   *
   * @param pathEntries The entries where the content should be injected
   */
  public void path(String... pathEntries) {
    this.pathEntries = pathEntries;
  }

  /**
   * Specifies what should happen when the injected content is already present in the target file.
   * Defaults to {@link JsonInjectionOverrideType#ALWAYS_ADD}.
   *
   * @param type The non-null type to specify what should happen when the entry is already present
   */
  public void override(JsonInjectionOverrideType type) {
    this.overrideType = type;
  }

  /**
   * Specifies the key where the content should be injected. Only necessary, if the content should be injected
   * into a json object. If it should be injected into an array, this method can be ignored.
   * <p>
   * Additionally, {@link #type(JsonInjectionType)} will automatically set to {@link JsonInjectionType#MODIFY_OBJECT}.
   *
   * @param injectKey The non-null key where the content should be injected
   */
  public void key(String injectKey) {
    this.type = JsonInjectionType.MODIFY_OBJECT;
    this.injectKey = injectKey;
  }

  /**
   * Sets the type of the json element where the content should be injected.
   * Defaults to {@link JsonInjectionType#MODIFY_ARRAY}.
   *
   * @param type The non-null type
   */
  public void type(JsonInjectionType type) {
    this.type = type;
  }

  /**
   * Sets the relative target path of this json injection.
   *
   * @param target The target path
   */
  public void to(Object target) {
    this.path = target.toString();
  }

  /**
   * Sets the relative target directory of this json injection, the name is used as the file name inside the directory.
   *
   * @param target The target directory path
   */
  public void into(Object target) {
    this.path = target.toString() + "/" + this.name;
  }

  /**
   * Checks this description for validity.
   *
   * @throws IllegalStateException If this description is not valid
   */
  public void validate() {
    if (this.injection == null) {
      throw new IllegalStateException("Missing injection, from() or inject() has not been called");
    }

    if (this.path == null) {
      throw new IllegalStateException("Missing path, to() or into() has not been called");
    }

    if (this.type == null) {
      throw new IllegalStateException("Missing type, type() has been set to null");
    }

    if (this.type == JsonInjectionType.MODIFY_OBJECT && this.injectKey == null) {
      throw new IllegalStateException(
          "Missing injectKey, key() has not been called and type() has been set to MODIFY_OBJECT");
    }

    if (this.overrideType == null) {
      throw new IllegalStateException("Missing overrideType, override() has been set to null");
    }

    try {
      Path targetPath = Paths.get(this.path);
      if (targetPath.isAbsolute()) {
        throw new IllegalStateException("Expected the target path to be a relative path");
      }
    } catch (InvalidPathException e) {
      throw new IllegalStateException("Target path is invalid", e);
    }
  }

  /**
   * Retrieves the name of this json injection.
   *
   * @return The name of this json injection
   */
  @Nonnull
  public String getName() {
    return name;
  }

  /**
   * Retrieves the target path of this json injection.
   *
   * @return The target path
   */
  public String getPath() {
    return path;
  }

  /**
   * Retrieves whether the generated json should be printed just in one line or with line breaks.
   *
   * @return {@code true} if it should be printed with line breaks, {@code false} otherwise
   */
  public boolean isPrettyPrint() {
    return this.prettyPrint;
  }

  /**
   * Retrieves the key where the content should be injected. Only necessary, if the content should be injected
   * into a json object.
   *
   * @return The key where the content should be injected,
   * non-null if {@link #getType()} is {@link JsonInjectionType#MODIFY_OBJECT}
   * @see #key(String)
   */
  public String getInjectKey() {
    return this.injectKey;
  }

  /**
   * Retrieves what should happen when the injected content is already present in the target file.
   *
   * @return The non-null type to specify what should happen when the entry is already present
   */
  public JsonInjectionOverrideType getOverrideType() {
    return this.overrideType;
  }

  /**
   * Retrieves the type of the json element where the content should be injected.
   *
   * @return The non-null type
   */
  public JsonInjectionType getType() {
    return this.type;
  }

  /**
   * Retrieves the path in the json where the content should be injected.
   *
   * @return The entries where the content should be injected
   * @see #path(String...)
   */
  public String[] getPathEntries() {
    return this.pathEntries;
  }

  /**
   * Retrieves the json that should be injected into the target.
   *
   * @return The non-null json to be injected
   */
  public JsonElement getInjection() {
    return this.injection;
  }
}
