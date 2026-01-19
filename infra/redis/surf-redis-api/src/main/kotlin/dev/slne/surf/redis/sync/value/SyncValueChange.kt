package dev.slne.surf.redis.sync.value

/**
 * Change event emitted by [SyncValue] instances.
 */
sealed interface SyncValueChange {
    /**
     * Emitted when the value changes.
     *
     * @param new the new value after the update
     * @param old the previous value before the update
     */
    data class Updated<E : Any>(val new: E, val old: E) : SyncValueChange
}