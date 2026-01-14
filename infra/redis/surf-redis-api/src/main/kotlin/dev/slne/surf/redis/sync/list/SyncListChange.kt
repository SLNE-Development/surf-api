package dev.slne.surf.redis.sync.list

sealed interface SyncListChange<E> {
    data class Added<E>(val index: Int, val element: E) : SyncListChange<E>
    data class Appended<E>(val element: E) : SyncListChange<E>
    data class Removed<E>(val removed: E) : SyncListChange<E>
    data class RemovedAt<E>(val index: Int, val removed: E) : SyncListChange<E>
    data class Updated<E>(val index: Int, val new: E, val old: E) : SyncListChange<E>
    class Cleared<E> : SyncListChange<E>
}