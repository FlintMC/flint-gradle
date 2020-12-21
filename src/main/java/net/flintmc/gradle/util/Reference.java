package net.flintmc.gradle.util;

public class Reference<T> extends MaybeNull<T> {
  /**
   * Constructs a new {@link Reference}.
   *
   * @param value The contained value
   */
  public Reference(T value) {
    super(value);
  }

  /**
   * Sets the value of this reference.
   *
   * @param newValue The new value of this reference
   */
  public void set(T newValue) {
    value = newValue;
  }
}
