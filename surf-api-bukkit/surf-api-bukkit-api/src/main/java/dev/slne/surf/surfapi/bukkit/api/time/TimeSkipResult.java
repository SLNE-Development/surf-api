package dev.slne.surf.surfapi.bukkit.api.time;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

/**
 * Represents the result of a time skip operation.
 */
@ApiStatus.NonExtendable
public enum TimeSkipResult {
  /**
   * Represents the result of a time skip operation, indicating a successful operation.
   */
  SUCCESS(true),
  /**
   * Represents the result of a time skip operation, indicating a failed operation for any reason.
   */
  FAILED(false),
  /**
   * Represents the result of a time skip operation, indicating that the time skip operation was
   * already in progress.
   */
  ALREADY_SKIPPING(false);

  /**
   * Represents the success status of a time skip operation.
   * <p>
   * This variable is a boolean value indicating whether the time skip operation was successful or
   * not.
   * <p>
   * Example usage: {@snippet : boolean success = TimeSkipResult.toBoolean();}
   */
  private final boolean success;

  /**
   * Creates a TimeSkipResult object.
   *
   * @param success indicates whether the time skip operation was successful or not
   */
  @Contract(pure = true)
  TimeSkipResult(boolean success) {
    this.success = success;
  }

  /**
   * Converts the TimeSkipResult to a boolean value.
   *
   * @return true if the TimeSkipResult represents a successful time skip operation, false
   * otherwise.
   */
  @Contract(pure = true)
  public boolean toBoolean() {
    return success;
  }
}
