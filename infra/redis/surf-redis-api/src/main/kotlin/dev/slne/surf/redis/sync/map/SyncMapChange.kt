package dev.slne.surf.redis.sync.map

sealed interface SyncMapChange<K : Any, V : Any> {
    data class Put<K : Any, V : Any>(val key: K, val new: V, val old: V?) : SyncMapChange<K, V>
    data class Removed<K : Any, V : Any>(val key: K, val removed: V) : SyncMapChange<K, V>
    class Cleared<K : Any, V : Any> : SyncMapChange<K, V>
}