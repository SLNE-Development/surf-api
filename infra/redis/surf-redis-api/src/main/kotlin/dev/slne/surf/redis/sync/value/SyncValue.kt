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
 * ## Remote access
 * The `*Remote` methods bypass the eventually consistent local view and operate on the committed
 * Redis state. Mutations are applied atomically in Redis first; the local view is updated
 * afterwards. A Redis key that holds no value is observed as the default value.
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
     * Reads the committed value directly from Redis.
     *
     * When the remote state is ahead of the local view, the local value is replaced by the result.
     */
    suspend fun getRemote(): T

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
     * Updates the local value and waits until the update has been committed to Redis.
     *
     * This does not wait for other nodes to apply the corresponding stream event.
     */
    suspend fun setAndAwait(newValue: T)

    /**
     * Atomically replaces the Redis value with [newValue] if it currently equals [expectedValue].
     *
     * Equality is evaluated on the encoded representation. An absent Redis value matches
     * [expectedValue] when [expectedValue] equals the default value.
     *
     * @return `true` if the Redis value matched and was replaced
     */
    suspend fun compareAndSetRemote(expectedValue: T, newValue: T): Boolean

    /**
     * Atomically sets the Redis value to [newValue] and returns the value it replaced.
     */
    suspend fun getAndSetRemote(newValue: T): T

    /**
     * Atomically applies [transform] to the committed Redis value and returns the new value.
     *
     * Implemented as a compare-and-set loop that retries without bound while other nodes modify the
     * value concurrently. [transform] must be side-effect free and may be invoked more than once.
     */
    suspend fun updateAndGetRemote(transform: (T) -> T): T

    /**
     * Atomically applies [transform] to the committed Redis value and returns the previous value.
     *
     * Implemented as a compare-and-set loop that retries without bound while other nodes modify the
     * value concurrently. [transform] must be side-effect free and may be invoked more than once.
     */
    suspend fun getAndUpdateRemote(transform: (T) -> T): T

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
