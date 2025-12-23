package dev.slne.surf.redis.sync.value

sealed interface SyncValueChange {
    data class Updated<E : Any>(val new: E, val old: E) : SyncValueChange
}