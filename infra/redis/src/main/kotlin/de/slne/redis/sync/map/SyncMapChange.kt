package de.slne.redis.sync.map

sealed interface SyncMapChange {
    data class Put<K : Any, V : Any>(val key: K, val new: V, val old: V?) : SyncMapChange
    data class Removed<K : Any, V : Any>(val key: K, val removed: V) : SyncMapChange
    data object Cleared : SyncMapChange
}