package dev.slne.surf.redis.sync.set

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.SyncStructure
import dev.slne.surf.redis.sync.set.SyncSet.Companion.DEFAULT_TTL
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlin.time.Duration.Companion.minutes

/**
 * Replicated in-memory set that is kept in sync across all Redis-connected nodes.
 *
 * ## How it works
 * - **In-memory state:** each node holds a local `Set<T>` instance.
 * - **Delta-based replication:** mutations publish deltas via Redis Pub/Sub.
 * - **Snapshot for late joiners:** the full set is stored in Redis with a TTL.
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
 * - Mutating methods are **non-suspending** and do not block.
 * - Redis I/O is executed asynchronously on the provided [scope].
 * - Change listeners are invoked on the thread applying the change
 *   (caller thread for local changes, Redisson/Reactor thread for remote changes).
 *
 * ## Failure semantics
 * - If all nodes go offline, Redis state expires automatically after [ttl].
 * - The next node recreates the set from the provided operations.
 *
 * @param api owning [RedisApi] instance
 * @param id unique identifier for this set (defines Redis keys and channel)
 * @param scope coroutine scope used for asynchronous Redis operations
 * @param elementSerializer serializer for set elements
 * @param ttl TTL for snapshot and version keys (default: [DEFAULT_TTL])
 */
interface SyncSet<T : Any> : SyncStructure<SyncSetChange> {
    companion object {

        /**
         * Default TTL for snapshot and version keys.
         *
         * Refreshed periodically by the heartbeat while at least one node is alive.
         */
        val DEFAULT_TTL = 5.minutes
    }

    /**
     * Returns a copy of the current set state.
     *
     * The returned set is a snapshot and will not reflect future updates.
     */
    fun snapshot(): ObjectOpenHashSet<T>

    /** @return number of elements in the set */
    fun size(): Int

    /** @return `true` if [element] is present */
    fun contains(element: T): Boolean

    /**
     * Adds [element] to the set and replicates the change.
     *
     * @return `true` if the element was added, `false` if it was already present
     */
    fun add(element: T): Boolean

    /**
     * Removes [element] from the set and replicates the change.
     *
     * @return `true` if the element was removed, `false` if it was not present
     */
    fun remove(element: T): Boolean

    /**
     * Removes all elements from the set that match the given [predicate] and replicates the changes.
     *
     * This operation uses atomic replication: the entire set is replaced in a single delta.
     * This ensures consistency across distributed nodes since the operation cannot interleave
     * with other deltas, avoiding race conditions and ensuring all nodes see the same final state.
     *
     * @param predicate the predicate to test each element against
     * @return `true` if any elements were removed, `false` if no elements matched the predicate
     */
    fun removeIf(predicate: (T) -> Boolean): Boolean

    /**
     * Clears the set and replicates the change.
     *
     * If the set is already empty, this method is a no-op and does not publish a delta.
     */
    fun clear()
}
