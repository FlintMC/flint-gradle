package net.flintmc.gradle.util;

/**
 * Wrapper for possible {@code null} values. This is required because Gradle does not allow passing {@code null} to
 * injected constructors, which is required in some cases in this plugin.
 *
 * @param <T> The contained type
 */
public class MaybeNull<T> {
  protected T value;

  /**
   * Constructs a new {@link MaybeNull}.
   *
   * @param value The contained value
   */
  public MaybeNull(T value) {
    this.value = value;
  }

  /**
   * Retrieves the contained value
   *
   * @return The contained value, or {@code null}
   */
  public T get() {
    return value;
  }
}
