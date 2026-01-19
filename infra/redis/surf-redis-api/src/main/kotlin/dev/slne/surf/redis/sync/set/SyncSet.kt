package dev.slne.surf.redis.sync.set

import dev.slne.surf.redis.sync.SyncStructure
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlin.time.Duration.Companion.minutes

/**
 * Replicated in-memory set synchronized across Redis-connected nodes.
 *
 * A [SyncSet] exposes a local set-like view and propagates mutations through Redis so that other
 * nodes can observe and apply changes.
 *
 * Consumers should treat this structure as eventually consistent: updates may arrive later on other
 * nodes, and remote updates may overwrite the local state.
 *
 * ## Access
 * - [snapshot] returns a copy of the current local contents.
 * - [contains], [size], [add], [remove], [removeIf] and [clear] operate on the local view and
 *   propagate changes via Redis.
 *
 * ## Listeners
 * Listeners registered via [SyncStructure.addListener] receive [SyncSetChange] events for changes.
 * The thread used for listener invocation is implementation-defined.
 */
interface SyncSet<T : Any> : SyncStructure<SyncSetChange> {
    companion object {
        /**
         * Default TTL configuration used by implementations when creating a [SyncSet].
         */
        val DEFAULT_TTL = 5.minutes
    }

    /**
     * Returns a copy of the current local contents.
     *
     * The returned set is a snapshot and will not reflect future updates.
     */
    fun snapshot(): ObjectOpenHashSet<T>

    /**
     * @return the current local element count
     */
    fun size(): Int

    /**
     * Checks whether [element] is present in the local set view.
     */
    operator fun contains(element: T): Boolean

    /**
     * Adds [element] to the set and propagates the change through Redis.
     *
     * @return `true` if the element was added locally, `false` if it was already present
     */
    fun add(element: T): Boolean

    /**
     * Convenience operator for [add].
     */
    operator fun plusAssign(element: T) {
        add(element)
    }

    /**
     * Removes [element] from the set and propagates the change through Redis.
     *
     * @return `true` if the element was removed locally, `false` if it was not present
     */
    fun remove(element: T): Boolean

    /**
     * Convenience operator for [remove].
     */
    operator fun minusAssign(element: T) {
        remove(element)
    }

    /**
     * Removes all elements that match [predicate] and propagates the change through Redis.
     *
     * The propagation strategy is implementation-defined. Callers should assume that other nodes
     * will eventually converge to the same resulting set.
     *
     * @return `true` if any elements were removed locally, `false` otherwise
     */
    fun removeIf(predicate: (T) -> Boolean): Boolean

    /**
     * Clears the local set and propagates the change through Redis.
     *
     * If the local set is already empty, this method is a no-op.
     */
    fun clear()
}
