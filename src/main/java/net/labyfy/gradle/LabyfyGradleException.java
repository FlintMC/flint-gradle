package net.labyfy.gradle;

import org.gradle.api.GradleException;

import javax.annotation.Nullable;

public class LabyfyGradleException extends GradleException {
    public LabyfyGradleException() {
    }

    public LabyfyGradleException(String message) {
        super(message);
    }

    public LabyfyGradleException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
