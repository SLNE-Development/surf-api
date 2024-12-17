package dev.slne.surf.surfapi.bukkit.api.time

import org.jetbrains.annotations.ApiStatus

/**
 * Represents the result of a time skip operation.
 */
@ApiStatus.NonExtendable
enum class TimeSkipResult(
    /**
     * Represents the success status of a time skip operation.
     *
     *
     * This variable is a boolean value indicating whether the time skip operation was successful or
     * not.
     */
    private val success: Boolean
) {
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
     * Converts the TimeSkipResult to a boolean value.
     *
     * @return true if the TimeSkipResult represents a successful time skip operation, false
     * otherwise.
     */
    fun toBoolean(): Boolean {
        return success
    }
}
