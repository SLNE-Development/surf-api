package de.slne.redis.sync

/**
 * Listener interface for subscribing to changes in synchronized structures.
 * @param T The type of value being synchronized
 */
fun interface SyncChangeListener<T> {
    /**
     * Called when a change occurs in the synchronized structure.
     * @param changeType The type of change that occurred
     * @param value The new or affected value
     * @param key Optional key for map operations (null for non-map structures)
     */
    fun onChange(changeType: SyncChangeType, value: T, key: Any?)
}
