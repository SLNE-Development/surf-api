package dev.slne.surf.redis.sync.map

/**
 * Change event emitted by [SyncMap] instances.
 */
sealed interface SyncMapChange<K : Any, V : Any> {
    /**
     * Emitted when a value is associated with [key].
     *
     * @param key the affected key
     * @param new the new value
     * @param old the previous value, or `null` if there was no mapping
     */
    data class Put<K : Any, V : Any>(val key: K, val new: V, val old: V?) : SyncMapChange<K, V>

    /**
     * Emitted when an entry is removed.
     *
     * @param key the affected key
     * @param removed the removed value
     */
    data class Removed<K : Any, V : Any>(val key: K, val removed: V) : SyncMapChange<K, V>

    /**
     * Emitted when the map is cleared.
     */
    class Cleared<K : Any, V : Any> : SyncMapChange<K, V>
}