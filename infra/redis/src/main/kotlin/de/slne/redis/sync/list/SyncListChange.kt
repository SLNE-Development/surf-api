package de.slne.redis.sync.list

sealed interface SyncListChange {
    data class Added<E>(val index: Int, val element: E) : SyncListChange
    data class Removed<E>(val index: Int, val removed: E) : SyncListChange
    data class Updated<E>(val index: Int, val new: E, val old: E) : SyncListChange
    data object Cleared : SyncListChange
}