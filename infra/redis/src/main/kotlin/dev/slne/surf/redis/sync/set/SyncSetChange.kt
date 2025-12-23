package dev.slne.surf.redis.sync.set

sealed interface SyncSetChange {
    data class Added<E : Any>(val element: E) : SyncSetChange
    data class Removed<E : Any>(val element: E) : SyncSetChange
    data object Cleared : SyncSetChange
}