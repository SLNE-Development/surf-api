package dev.slne.surf.redis.sync.list

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.sync.SyncStructure
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlin.time.Duration.Companion.minutes

/**
 * Replicated in-memory list that is kept in sync across all Redis-connected nodes.
 *
 * ## How it works
 * - **In-memory state:** each node holds its own local list instance.
 * - **Deltas via Pub/Sub:** mutations publish a delta to [redisChannel].
 * - **Snapshot for late joiners:** the full list is stored in Redis under [dataKey] with a TTL.
 * - **Versioning:** each mutation increments [verKey]. Deltas carry a monotonically increasing version.
 * - **Resync:** if a node detects a version gap, it reloads the snapshot.
 * - **Ephemeral storage:** snapshot + version keys expire automatically. A heartbeat refreshes the TTL while
 *   at least one node is alive.
 *
 * ## Consistency / ordering
 * - Eventual consistency across nodes.
 * - Deltas are applied only if `version == localVersion + 1`.
 * - Duplicate/out-of-order deltas (`version <= localVersion`) are ignored.
 * - Missing deltas trigger a snapshot reload.
 *
 * ## Threading / API behavior
 * - Public mutating methods (e.g. [add], [set]) are **non-suspending** and do not block.
 * - Redis I/O is executed asynchronously on the provided [scope].
 * - Change listeners are invoked on the thread that applies the change
 *   (caller thread for local changes, Redisson/Reactor thread for remote changes).
 *
 * @param api owning [RedisApi] (must already be connected and have Pub/Sub available)
 * @param id unique identifier for this list (defines Redis keys and channel)
 * @param scope coroutine scope used for asynchronous Redis I/O and background tasks (heartbeat/resync)
 * @param elementSerializer serializer for list elements
 * @param ttl TTL for snapshot/version keys; refreshed periodically while the list is active
 */
interface SyncList<T : Any> : SyncStructure<SyncListChange<T>> {

    companion object {

        /**
         * Default TTL for the snapshot and version keys.
         *
         * The heartbeat refreshes the TTL periodically to keep the list ephemeral.
         */
        val DEFAULT_TTL = 5.minutes
    }


    /**
     * Returns a copy of the current list state.
     *
     * The returned list is a snapshot and will not reflect later updates.
     */
    fun snapshot(): ObjectArrayList<T>

    /** @return current number of elements in the list */
    fun size(): Int

    /** @return element at [index] */
    operator fun get(index: Int): T


    operator fun contains(element: T): Boolean

    /**
     * Appends [element] to the end of the list.
     *
     * The local list is updated immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     */
    fun add(element: T)
    operator fun plusAssign(element: T) = add(element)

    fun remove(element: T): Boolean
    operator fun minusAssign(element: T) {
        remove(element)
    }

    /**
     * Replaces the element at [index] with [element].
     *
     * The local list is updated immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     *
     * @return the previous value at [index]
     */
    operator fun set(index: Int, element: T): T

    /**
     * Removes the element at [index].
     *
     * The local list is updated immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     *
     * @return the removed element
     */
    fun removeAt(index: Int): T

    /**
     * Removes all elements from the list that match the given [predicate] and replicates the changes.
     *
     * This operation uses atomic replication: the entire list is replaced in a single delta.
     * This ensures consistency across distributed nodes since the operation cannot interleave
     * with other deltas in the middle of a Clear/Add sequence, avoiding ordering divergence.
     *
     * @param predicate the predicate to test each element against
     * @return `true` if any elements were removed, `false` if no elements matched the predicate
     */
    fun removeIf(predicate: (T) -> Boolean): Boolean

    /**
     * Clears the list.
     *
     * The local list is cleared immediately and listeners are notified.
     * Replication to other nodes happens asynchronously.
     */
    fun clear()
}