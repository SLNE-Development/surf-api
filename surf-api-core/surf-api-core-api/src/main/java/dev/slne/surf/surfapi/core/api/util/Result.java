package dev.slne.surf.surfapi.core.api.util;

import java.util.function.Consumer;


/**
 * The Result class is a generic class that represents the result of a computation. It provides
 * methods to complete the result and to register callback that will be executed when the result is
 * complete.
 *
 * @param <T> the type of the result value
 */
public class Result<T> {

  private T value;
  private Consumer<T> whenComplete;

  /**
   * The Result class is a generic class that represents the result of a computation. It provides
   * methods to complete the result and to register a callback that will be executed when the result
   * is complete.
   */
  public Result() {
  }

  /**
   * Constructor for creating a Result object with an initial value.
   *
   * @param value the initial value of the Result object
   */
  public Result(T value) {
    this.value = value;
  }

  /**
   * Completes the result with the given value. Sets the value of the result to the specified value
   * and invokes the registered callback, if any.
   *
   * @param value the value to complete the result with
   */
  public void complete(T value) {
    this.value = value;
    if (whenComplete != null) {
      whenComplete.accept(value);
    }
  }

  /**
   * Registers a callback function to be executed when the result is complete.
   *
   * @param whenComplete the callback function to be executed
   */
  public void whenComplete(Consumer<T> whenComplete) {
    this.whenComplete = whenComplete;
    if (value != null) {
      whenComplete.accept(value);
    }
  }
}
