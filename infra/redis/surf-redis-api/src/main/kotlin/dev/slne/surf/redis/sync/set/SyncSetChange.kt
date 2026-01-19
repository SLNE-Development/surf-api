package dev.slne.surf.redis.sync.set

/**
 * Change event emitted by [SyncSet] instances.
 */
sealed interface SyncSetChange {
    /**
     * Emitted when an element is added.
     */
    data class Added<E : Any>(val element: E) : SyncSetChange

    /**
     * Emitted when an element is removed.
     */
    data class Removed<E : Any>(val element: E) : SyncSetChange

    /**
     * Emitted when the set is cleared.
     */
    data object Cleared : SyncSetChange
}