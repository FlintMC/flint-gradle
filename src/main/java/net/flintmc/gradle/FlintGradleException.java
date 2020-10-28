package net.flintmc.gradle;

import org.gradle.api.GradleException;

import javax.annotation.Nullable;

public class FlintGradleException extends GradleException {
  public FlintGradleException() {
  }

  public FlintGradleException(String message) {
    super(message);
  }

  public FlintGradleException(String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
