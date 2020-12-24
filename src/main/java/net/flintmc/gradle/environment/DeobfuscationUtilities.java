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

package net.flintmc.gradle.environment;

import net.flintmc.gradle.java.compile.JavaCompileHelper;
import net.flintmc.gradle.java.exec.JavaExecutionHelper;
import net.flintmc.gradle.maven.MavenArtifactDownloader;
import net.flintmc.gradle.maven.SimpleMavenRepository;
import net.flintmc.gradle.minecraft.MinecraftRepository;
import okhttp3.OkHttpClient;

/**
 * Class holding utilities useful during deobfuscation
 */
public class DeobfuscationUtilities {
  private final MavenArtifactDownloader downloader;
  private final MinecraftRepository minecraftRepository;
  private final SimpleMavenRepository internalRepository;
  private final OkHttpClient httpClient;
  private final EnvironmentCacheFileProvider cacheFileProvider;
  private final JavaExecutionHelper javaExecutionHelper;
  private final JavaCompileHelper javaCompileHelper;

  /**
   * Constructs a new pack of deobfuscation utilities.
   *
   * @param downloader          The maven artifact download to make available to the deobfuscation environment
   * @param minecraftRepository The repository where minecraft artifacts can be found and should be places
   * @param internalRepository  The repository where minecraft dependencies can be found and internal dependencies
   *                            should be placed
   * @param httpClient          The HTTP client which should be used for downloads during deobfuscation, may be null
   *                            when operating in offline mode
   * @param cacheFileProvider   The cache file provider to make available to the deobfuscation environment
   * @param javaExecutionHelper The execution helper to use for invoking java processes
   * @param javaCompileHelper   The compile helper to use for compiling and packaging java archives
   */
  public DeobfuscationUtilities(
      MavenArtifactDownloader downloader,
      MinecraftRepository minecraftRepository,
      SimpleMavenRepository internalRepository,
      OkHttpClient httpClient,
      EnvironmentCacheFileProvider cacheFileProvider,
      JavaExecutionHelper javaExecutionHelper,
      JavaCompileHelper javaCompileHelper
  ) {
    this.downloader = downloader;
    this.minecraftRepository = minecraftRepository;
    this.internalRepository = internalRepository;
    this.httpClient = httpClient;
    this.cacheFileProvider = cacheFileProvider;
    this.javaExecutionHelper = javaExecutionHelper;
    this.javaCompileHelper = javaCompileHelper;
  }

  /**
   * Retrieves the downloader used for downloading maven artifacts
   *
   * @return The downloader used for downloading maven artifacts
   */
  public MavenArtifactDownloader getDownloader() {
    return downloader;
  }

  /**
   * Retrieves the repository where minecraft artifacts can be found.
   *
   * @return The repository where minecraft artifacts can be found
   */
  public MinecraftRepository getMinecraftRepository() {
    return minecraftRepository;
  }

  /**
   * Retrieves the repository where minecraft and internal dependencies can be found
   *
   * @return Repositories where dependencies required for obfuscation can be found
   */
  public SimpleMavenRepository getInternalRepository() {
    return internalRepository;
  }

  /**
   * Retrieves the HTTP client which should be used for downloads during deobfuscation.
   *
   * @return The HTTP client which should be used
   */
  public OkHttpClient getHttpClient() {
    return httpClient;
  }

  /**
   * Retrieves the cache file provider which should be user for caching files.
   *
   * @return The cache file provider
   */
  public EnvironmentCacheFileProvider getCacheFileProvider() {
    return cacheFileProvider;
  }

  /**
   * Retrieves the helper for executing java processes.
   *
   * @return The helper for executing java processes
   */
  public JavaExecutionHelper getJavaExecutionHelper() {
    return javaExecutionHelper;
  }

  /**
   * Retrieves the helper for compiling and packaging java archives.
   *
   * @return The helper for compiling and packaging java archives
   */
  public JavaCompileHelper getJavaCompileHelper() {
    return javaCompileHelper;
  }
}
