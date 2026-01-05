package dev.slne.surf.redis.sync.map

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.SyncStructure
import dev.slne.surf.redis.sync.map.SyncMap.Companion.DEFAULT_TTL
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlin.time.Duration.Companion.minutes

/**
 * Replicated in-memory map that is kept in sync across all Redis-connected nodes.
 *
 * ## How it works
 * - **In-memory state:** each node holds a local `Map<K, V>` instance.
 * - **Delta-based replication:** mutations publish deltas via Redis Pub/Sub.
 * - **Snapshot for late joiners:** the full map is stored in Redis with a TTL.
 * - **Versioning:** every mutation increments a global version counter.
 * - **Resync:** if a node detects a version gap, it reloads the snapshot.
 * - **Ephemeral storage:** snapshot and version keys expire automatically and
 *   are refreshed via heartbeat while nodes are alive.
 *
 * ## Consistency model
 * - Eventual consistency.
 * - Deltas are applied only if `version == localVersion + 1`.
 * - Duplicate or out-of-order deltas are ignored.
 * - Missing deltas trigger a snapshot reload.
 *
 * ## Threading / API behavior
 * - Public mutating methods are **non-suspending** and never block.
 * - Redis I/O is executed asynchronously on the provided [scope].
 * - Change listeners are invoked on the thread applying the change
 *   (caller thread for local changes, Redisson/Reactor thread for remote changes).
 *
 * ## Failure semantics
 * - If all nodes go offline, Redis state expires automatically after [ttl].
 * - The next node recreates the map from the provided operations.
 *
 * @param api owning [RedisApi] instance
 * @param id unique identifier for this map (defines Redis keys and channel)
 * @param scope coroutine scope used for asynchronous Redis operations
 * @param keySerializer serializer for map keys
 * @param valueSerializer serializer for map values
 * @param ttl TTL for snapshot and version keys (default: [DEFAULT_TTL])
 */
interface SyncMap<K : Any, V : Any> : SyncStructure<SyncMapChange<K, V>> {

    companion object {
        /**
         * Default TTL for snapshot and version keys.
         *
         * Refreshed periodically by the heartbeat while at least one node is alive.
         */
        val DEFAULT_TTL = 5.minutes
    }


    /**
     * Returns a copy of the current map state.
     *
     * The returned map is a snapshot and will not reflect future updates.
     */
    fun snapshot(): Object2ObjectOpenHashMap<K, V>

    /** @return number of entries in the map */
    fun size(): Int

    /** @return `true` if the map is empty */
    fun isEmpty(): Boolean

    /** @return value associated with [key], or `null` if absent */
    fun get(key: K): V?

    /** @return `true` if the map contains [key] */
    fun containsKey(key: K): Boolean

    /**
     * Associates [value] with [key] in the map and replicates the change.
     *
     * The local map is updated immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     *
     * @return the previous value associated with [key], or `null` if there was no mapping
     */
    fun put(key: K, value: V): V?

    /**
     * Removes the mapping for [key] from the map and replicates the change.
     *
     * The local map is updated immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     *
     * @return the value that was associated with [key], or `null` if there was no mapping
     */
    fun remove(key: K): V?

    fun removeIf(predicate: (K, V) -> Boolean): Boolean

    /**
     * Removes all mappings from the map and replicates the change.
     *
     * The local map is cleared immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     *
     * If the map is already empty, this method is a no-op and does not publish a delta.
     */
    fun clear()
}
