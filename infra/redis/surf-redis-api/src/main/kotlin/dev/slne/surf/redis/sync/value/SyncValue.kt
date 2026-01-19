package dev.slne.surf.redis.sync.value

import dev.slne.surf.redis.sync.SyncStructure
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.minutes

/**
 * Replicated in-memory value synchronized across Redis-connected nodes.
 *
 * This implementation keeps a local in-memory copy and stores a snapshot in Redis.
 * Remote changes are detected via Redisson tracking and reflected back into the local state.
 *
 * ## Model
 * - **Local state:** each node holds a local value of type [T].
 * - **Remote snapshot:** the current value is stored as JSON in Redis under a snapshot key.
 * - **Change propagation:** when the snapshot key changes, nodes reload it and update local state.
 * - **TTL / heartbeat:** the snapshot key is written with a TTL and periodically refreshed.
 *
 * ## Consistency
 * - Eventual consistency ("last write wins" at the Redis snapshot key).
 * - Updates are applied by reloading the snapshot on change notifications.
 *
 * ## Threading
 * - [get] is a fast local read.
 * - [set] updates local state immediately and writes to Redis asynchronously.
 * - Listener callbacks are invoked on the thread that applies the change
 *   (caller thread for local changes, Redisson/Reactor thread for remote changes).
 */
interface SyncValue<T : Any> : SyncStructure<SyncValueChange> {
    companion object {

        /** Default TTL for the snapshot key. Refreshed periodically by the heartbeat. */
        val DEFAULT_TTL = 5.minutes
    }

    /** Returns the current local value. */
    fun get(): T

    /**
     * Updates the value locally and writes the new snapshot to Redis.
     *
     * This method returns immediately; Redis I/O happens asynchronously.
     * Listeners are notified for both local and remote updates.
     */
    fun set(newValue: T)

    /**
     * Exposes this [SyncValue] as a Kotlin property delegate.
     *
     * The returned [ReadWriteProperty] forwards:
     * - reads (`getValue`) to [get]
     * - writes (`setValue`) to [set]
     *
     * This allows the synchronized value to be accessed and modified using
     * standard Kotlin `by`-delegation syntax while preserving the full
     * synchronization semantics of [SyncValue].
     *
     * ### Example
     * ```
     * val counter: SyncValue<Int> = ...
     *
     * var value by counter.asProperty()
     *
     * value += 1 // updates local state and writes to Redis
     * ```
     *
     * @return a read-write property delegate backed by this [SyncValue]
     */
    fun asProperty() = object : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = get()
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
    }
}