package dev.slne.surf.redis.sync.list

import dev.slne.surf.redis.RedisApi
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
     * Removes the element at [index] and propagates the change through Redis.
     *
     * @return the removed element
     */
    fun removeAt(index: Int): T

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
     * Clears the local list and propagates the change through Redis.
     *
     * If the local list is already empty, this method is a no-op.
     */
    fun clear()
}