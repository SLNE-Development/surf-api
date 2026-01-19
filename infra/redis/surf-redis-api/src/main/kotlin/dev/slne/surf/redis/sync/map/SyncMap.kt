package dev.slne.surf.redis.sync.map

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.SyncStructure
import dev.slne.surf.redis.sync.map.SyncMap.Companion.DEFAULT_TTL
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlin.time.Duration.Companion.minutes

/**
 * Replicated in-memory map synchronized across Redis-connected nodes.
 *
 * A [SyncMap] exposes a local map-like view and propagates mutations through Redis so that other
 * nodes can observe and apply changes.
 *
 * Consumers should treat this structure as eventually consistent: updates may arrive later on other
 * nodes, and remote updates may overwrite the local state.
 *
 * ## Access
 * - [snapshot] returns a copy of the current local contents.
 * - [get], [containsKey], [size], [isEmpty] operate on the local view.
 * - [put], [remove], [removeIf] and [clear] mutate the local view and propagate changes via Redis.
 *
 * ## Listeners
 * Listeners registered via [SyncStructure.addListener] receive [SyncMapChange] events for changes.
 * The thread used for listener invocation is implementation-defined.
 */
interface SyncMap<K : Any, V : Any> : SyncStructure<SyncMapChange<K, V>> {

    companion object {
        /**
         * Default TTL configuration used by implementations when creating a [SyncMap].
         */
        val DEFAULT_TTL = 5.minutes
    }

    /**
     * Returns a copy of the current local contents.
     *
     * The returned map is a snapshot and will not reflect future updates.
     */
    fun snapshot(): Object2ObjectOpenHashMap<K, V>

    /**
     * @return the current local entry count
     */
    fun size(): Int

    /**
     * @return `true` if the local map is empty
     */
    fun isEmpty(): Boolean

    /**
     * Returns the value associated with [key] in the local map, or `null` if absent.
     */
    operator fun get(key: K): V?

    /**
     * Checks whether [key] is present in the local map.
     */
    fun containsKey(key: K): Boolean

    /**
     * Convenience operator for [containsKey].
     */
    operator fun contains(key: K): Boolean = containsKey(key)

    /**
     * Associates [value] with [key] in the local map and propagates the change through Redis.
     *
     * @return the previous value associated with [key], or `null` if there was no mapping
     */
   fun put(key: K, value: V): V?

    /**
     * Convenience operator for [put].
     */
   operator fun set(key: K, value: V): V? = put(key, value)

    /**
     * Removes the mapping for [key] from the local map and propagates the change through Redis.
     *
     * @return the previously associated value, or `null` if there was no mapping
     */
    fun remove(key: K): V?

    /**
     * Removes all entries that match [predicate] and propagates the change through Redis.
     *
     * The propagation strategy is implementation-defined. Callers should assume that other nodes
     * will eventually converge to the same resulting map.
     *
     * @return `true` if any entries were removed locally, `false` otherwise
     */
    fun removeIf(predicate: (K, V) -> Boolean): Boolean

    /**
     * Clears the local map and propagates the change through Redis.
     *
     * If the local map is already empty, this method is a no-op.
     */
    fun clear()
}
