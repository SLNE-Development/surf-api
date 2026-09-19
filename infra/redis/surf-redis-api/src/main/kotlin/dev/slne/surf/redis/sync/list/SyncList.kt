package dev.slne.surf.redis.sync.list

import dev.slne.surf.redis.sync.SyncStructure
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlin.time.Duration.Companion.minutes

/**
 * Replicated in-memory list synchronized across Redis-connected nodes.
 *
 * A [SyncList] exposes a local list-like view and propagates mutations through Redis so that other
 * nodes can observe and apply changes.
 *
 * Consumers should treat this structure as eventually consistent: updates may arrive later on other
 * nodes, and remote updates may overwrite the local state.
 *
 * ## Access
 * - [snapshot] returns a copy of the current local contents.
 * - [get], [contains], [size] operate on the local view.
 * - [add], [remove], [removeAt], [set], [removeIf] and [clear] mutate the local view and propagate
 *   changes via Redis.
 *
 * ## Remote access
 * The `*Remote` methods bypass the eventually consistent local view and operate on the committed
 * Redis state. Point reads leave the local view untouched.
 * [snapshotRemote] refreshes the local view when Redis is ahead of it. Mutations are applied
 * atomically in Redis first and report what Redis observed; the local view is updated afterward
 * and listeners are notified as for any other change.
 *
 * ## Listeners
 * Listeners registered via [SyncStructure.addListener] receive [SyncListChange] events for changes.
 * The thread used for listener invocation is implementation-defined.
 */
interface SyncList<T : Any> : SyncStructure<SyncListChange<T>> {

    companion object {
        /**
         * Default TTL configuration used by implementations when creating a [SyncList].
         */
        val DEFAULT_TTL = 5.minutes
    }

    /**
     * Returns a copy of the current local contents.
     *
     * The returned list is a snapshot and will not reflect future updates.
     */
    fun snapshot(): ObjectArrayList<T>

    /**
     * @return the current local element count
     */
    fun size(): Int

    /**
     * Returns the element at [index] from the local list view.
     */
    operator fun get(index: Int): T

    /**
     * Checks whether [element] is present in the local list view.
     */
    operator fun contains(element: T): Boolean

    /**
     * Appends [element] to the local list and propagates the change through Redis.
     */
    fun add(element: T)

    /**
     * Appends [element] locally and waits until the mutation has been committed to Redis.
     */
    suspend fun addAndAwait(element: T)

    /**
     * Convenience operator for [add].
     */
    operator fun plusAssign(element: T) = add(element)

    /**
     * Removes one occurrence of [element] from the local list and propagates the change through Redis.
     *
     * @return `true` if an element was removed locally, `false` otherwise
     */
    fun remove(element: T): Boolean

    /**
     * Removes one occurrence of [element] locally and waits until the mutation has been committed to Redis.
     *
     * @return `true` if an element was removed locally, `false` otherwise
     */
    suspend fun removeAndAwait(element: T): Boolean

    /**
     * Convenience operator for [remove].
     */
    operator fun minusAssign(element: T) {
        remove(element)
    }

    /**
     * Replaces the element at [index] with [element] and propagates the change through Redis.
     *
     * @return the previous element at [index]
     */
    operator fun set(index: Int, element: T): T

    /**
     * Replaces the element at [index] locally and waits until the mutation has been committed to Redis.
     *
     * @return the previous element at [index]
     */
    suspend fun setAndAwait(index: Int, element: T): T

    /**
     * Removes the element at [index] and propagates the change through Redis.
     *
     * @return the removed element
     */
    fun removeAt(index: Int): T

    /**
     * Removes the element at [index] locally and waits until the mutation has been committed to Redis.
     *
     * @return the removed element
     */
    suspend fun removeAtAndAwait(index: Int): T

    /**
     * Removes all elements that match [predicate] and propagates the change through Redis.
     *
     * The propagation strategy is implementation-defined. Callers should assume that other nodes
     * will eventually converge to the same resulting list.
     *
     * @return `true` if any elements were removed locally, `false` otherwise
     */
    fun removeIf(predicate: (T) -> Boolean): Boolean

    /**
     * Removes all locally matching elements and waits until the batched Redis mutation has completed.
     */
    suspend fun removeIfAndAwait(predicate: (T) -> Boolean): Boolean

    /**
     * Clears the local list and propagates the change through Redis.
     *
     * If the local list is already empty, this method is a no-op.
     */
    fun clear()

    /**
     * Clears the local list and waits until the clear has been committed to Redis.
     */
    suspend fun clearAndAwait()

    /**
     * Returns the element at [index] from Redis, bypassing the local view.
     *
     * @return the element, or `null` if [index] is out of range in Redis
     */
    suspend fun getRemote(index: Int): T?

    /**
     * Checks whether [element] is present in Redis, bypassing the local view.
     */
    suspend fun containsRemote(element: T): Boolean

    /**
     * Returns the element count in Redis, bypassing the local view.
     */
    suspend fun sizeRemote(): Int

    /**
     * Returns a copy of the committed Redis contents.
     *
     * When the remote state is ahead of the local view, the local view is replaced by the result.
     */
    suspend fun snapshotRemote(): ObjectArrayList<T>

    /**
     * Appends [element] to the Redis list and waits until the mutation has been committed.
     */
    suspend fun addRemote(element: T)

    /**
     * Replaces the element at [index] in Redis and returns the element Redis held before.
     *
     * @return the previous Redis element, or `null` if [index] is out of range in Redis
     */
    suspend fun setRemote(index: Int, element: T): T?

    /**
     * Removes one occurrence of [element] from the Redis list and waits until the mutation has been
     * committed.
     *
     * @return `true` if Redis contained [element], `false` otherwise
     */
    suspend fun removeRemote(element: T): Boolean

    /**
     * Removes the element at [index] from Redis and returns it.
     *
     * @return the removed Redis element, or `null` if [index] is out of range in Redis
     */
    suspend fun removeAtRemote(index: Int): T?

    /**
     * Clears the Redis list regardless of the local view and waits until the clear has been committed.
     *
     * Unlike [clearAndAwait], this is not skipped when the local view is already empty.
     */
    suspend fun clearRemote()
}