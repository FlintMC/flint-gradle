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

package net.flintmc.gradle.json;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.flintmc.installer.impl.repository.models.InternalModelSerializer;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for converting json input data to Java objects.
 */
public class JsonConverter {
  /**
   * Global mapper used for conversion. Applying options to it affects
   * all operations done with the {@link JsonConverter} class.
   */
  public static final ObjectMapper OBJECT_MAPPER;
  public static final InternalModelSerializer PACKAGE_MODEL_SERIALIZER = new InternalModelSerializer();

  static {
    OBJECT_MAPPER = new ObjectMapper();
    OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  private JsonConverter() {
  }

  /**
   * Converts the data from an {@link InputStream} to a Java object.
   *
   * @param stream      The stream to read data from
   * @param targetClass The class the target object should be of
   * @param <T>         The type the target object should be of
   * @return The data from the stream converted to the type of the target object
   * @throws JsonConverterException If an error occurs while creating the object,
   *                                such as IO errors or invalid json
   */
  public static <T> T streamToObject(InputStream stream, Class<T> targetClass) throws JsonConverterException {
    return streamToObject(stream, OBJECT_MAPPER.getTypeFactory().constructType(targetClass));
  }

  /**
   * Converts the data from an {@link InputStream} to a Java object.
   *
   * @param stream     The stream to read data from
   * @param targetType The type the target object should be of
   * @param <T>        The type the target object should be of
   * @return The data from the stream converted to the type of the target object
   * @throws JsonConverterException If an error occurs while creating the object,
   *                                such as IO errors or invalid json
   */
  public static <T> T streamToObject(InputStream stream, JavaType targetType) throws JsonConverterException {
    try {
      return OBJECT_MAPPER.readValue(stream, targetType);
    } catch (JsonParseException e) {
      throw new JsonConverterException("Got malformed json", e);
    } catch (JsonMappingException e) {
      throw new JsonConverterException("JsonMappingException while converting object", e);
    } catch (IOException e) {
      throw new JsonConverterException("IOException while reading from stream", e);
    }
  }
}