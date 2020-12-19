package net.flintmc.gradle.util;

import groovy.lang.Closure;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Helper to convert Java functional interfaces to closures.
 * <p>Some gradle methods take closures and have no interface version, this is where this class comes in useful.
 */
public abstract class JavaClosure<T> extends Closure<T> {
  public JavaClosure(Object owner, Object thisObject) {
    super(owner, thisObject);
  }

  /**
   * Wraps a Java {@link Runnable} as a groovy closure.
   *
   * @param runnable The runnable to wrap
   * @return The runnable wrapped as a groovy closure
   */
  public static JavaClosure<Void> of(Runnable runnable) {
    return new JavaClosure<Void>(runnable, null) {
      @SuppressWarnings("unused") // Called by groovy
      public void doCall() {
        runnable.run();
      }
    };
  }

  /**
   * Wraps a Java {@link Consumer} as a groovy closure.
   *
   * @param consumer The consumer to wrap
   * @param <C>      The parameter type of the consumer
   * @return The consumer wrapped as a groovy closure
   */
  public static <C> JavaClosure<Void> of(Consumer<C> consumer) {
    return new JavaClosure<Void>(consumer, null) {
      @SuppressWarnings("unused") // Called by groovy
      public void doCall(C param) {
        consumer.accept(param);
      }
    };
  }

  /**
   * Wraps a Java {@link Function} as a groovy closure.
   *
   * @param function The function to wrap
   * @param <C>      The parameter type of the function
   * @param <R>      The return type of the function
   * @return The wrapped function as a groovy closure
   */
  public static <C, R> JavaClosure<R> of(Function<C, R> function) {
    return new JavaClosure<R>(function, null) {
      @SuppressWarnings("unused") // Called by groovy
      public R doCall(C param) {
        return function.apply(param);
      }
    };
  }

  /**
   * Wraps a Java {@link Supplier} as a groovy closure.
   *
   * @param supplier The supplier to wrap
   * @param <R>      The return type of the supplier
   * @return The wrapper supplier as a groovy closure
   */
  public static <R> JavaClosure<R> of(Supplier<R> supplier) {
    return new JavaClosure<R>(supplier, null) {
      @SuppressWarnings("unused") // Called by groovy
      public R doCall() {
        return supplier.get();
      }
    };
  }
}
