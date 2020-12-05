package net.flintmc.gradle.util;

/**
 * Generic pair class
 *
 * @param <A> The type of the first element
 * @param <B> The type of the second element
 */
public class Pair<A, B> {
  private final A first;
  private final B second;

  /**
   * Constructs a new {@link Pair} with the given values.
   *
   * @param first  The first value
   * @param second The second value
   */
  public Pair(A first, B second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Retrieves the first value.
   *
   * @return The first value
   */
  public A getFirst() {
    return first;
  }

  /**
   * Retrieves the second value.
   *
   * @return The second value
   */
  public B getSecond() {
    return second;
  }
}
