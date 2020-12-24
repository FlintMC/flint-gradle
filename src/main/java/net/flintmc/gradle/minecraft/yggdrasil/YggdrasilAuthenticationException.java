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

package net.flintmc.gradle.minecraft.yggdrasil;

/**
 * Exception indicating that authentication failed.
 */
public class YggdrasilAuthenticationException extends Exception {
  /**
   * Constructs a new {@link YggdrasilAuthenticationException} with the given message.
   *
   * @param message The message explaining why the authentication failed
   */
  public YggdrasilAuthenticationException(String message) {
    super(message);
  }

  /**
   * Constructs a new {@link YggdrasilAuthenticationException} with the given message and cause.
   *
   * @param message The message explaining why authentication failed
   * @param cause   The cause of this exception
   */
  public YggdrasilAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
