package dev.slne.surf.redis.sync.list

/**
 * Change event emitted by [SyncList] instances.
 */
sealed interface SyncListChange<E> {

    /**
     * Emitted when an element is inserted at [index].
     */
    data class Added<E>(val index: Int, val element: E) : SyncListChange<E>

    /**
     * Emitted when an element is appended to the list.
     */
    data class Appended<E>(val element: E) : SyncListChange<E>

    /**
     * Emitted when an element is removed from the list.
     */
    data class Removed<E>(val removed: E) : SyncListChange<E>

    /**
     * Emitted when an element at [index] is removed.
     */
    data class RemovedAt<E>(val index: Int, val removed: E) : SyncListChange<E>

    /**
     * Emitted when an element at [index] is replaced.
     *
     * @param new the new element
     * @param old the previous element
     */
    data class Updated<E>(val index: Int, val new: E, val old: E) : SyncListChange<E>

    /**
     * Emitted when the list is cleared.
     */
    class Cleared<E> : SyncListChange<E>
}