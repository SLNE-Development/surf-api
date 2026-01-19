package dev.slne.surf.redis.sync.value

import dev.slne.surf.redis.sync.SyncStructure
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.minutes

/**
 * Replicated in-memory value synchronized across Redis-connected nodes.
 *
 * A [SyncValue] exposes a local value of type [T] and propagates updates through Redis so that other
 * nodes can observe and apply changes.
 *
 * The exact replication strategy is implementation-defined. Consumers should treat this structure as
 * eventually consistent: updates may arrive later on other nodes, and remote updates may overwrite
 * the local value.
 *
 * ## Access
 * - [get] reads the current local value.
 * - [set] updates the local value and triggers propagation to Redis.
 *
 * ## Listeners
 * Listeners registered via [SyncStructure.addListener] receive [SyncValueChange] events for updates.
 * The thread used for listener invocation is implementation-defined.
 */
interface SyncValue<T : Any> : SyncStructure<SyncValueChange> {
    companion object {
        /**
         * Default TTL configuration used by implementations when creating a [SyncValue].
         */
        val DEFAULT_TTL = 5.minutes
    }

    /**
     * Returns the current local value.
     */
    fun get(): T

    /**
     * Updates the local value and propagates the change through Redis.
     *
     * The propagation mechanism is implementation-defined. This method may return before the update
     * is observed by other nodes.
     *
     * @param newValue the new value to set
     */
    fun set(newValue: T)

    /**
     * Exposes this [SyncValue] as a Kotlin property delegate.
     *
     * Reads (`getValue`) are forwarded to [get], writes (`setValue`) are forwarded to [set].
     *
     * ## Example
     * ```
     * val counter: SyncValue<Int> = ...
     * var value by counter.asProperty()
     *
     * value += 1
     * ```
     */
    fun asProperty() = object : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = get()
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
    }
}