package dev.slne.surf.redis.sync

import dev.slne.surf.redis.util.InternalRedisAPI
import reactor.core.Disposable
import reactor.core.publisher.Mono
import kotlin.time.Duration

/**
 * Base contract for Redis-backed synchronization structures.
 *
 * A [SyncStructure] represents an object that maintains local state and keeps it in sync with Redis.
 * Implementations are expected to be created during application startup and then initialized by the
 * owning API.
 *
 * ## Identity
 * [id] is a logical identifier used by implementations to derive Redis keys and/or channels.
 *
 * ## TTL / heartbeat
 * [ttl] is used by implementations to refresh remote state periodically. A non-positive TTL may be
 * treated as "no TTL/heartbeat" depending on the implementation.
 *
 * ## Lifecycle
 * Implementations are initialized via [init] and must be disposed via [Disposable.dispose].
 *
 * ## Listeners
 * Structures expose a lightweight listener mechanism to deliver change events of type [L] to
 * consumers. Listener invocation and threading are implementation-defined.
 */
interface SyncStructure<L> : Disposable {

    /**
     * Logical structure identifier (used to derive Redis keys/channels).
     */
    val id: String

    /**
     * Time-to-live/heartbeat configuration used by the implementation.
     */
    val ttl: Duration

    /**
     * Internal initialization hook invoked by surf-redis during startup.
     *
     * Implementations typically use this to:
     * - register Redis listeners
     * - load initial remote state
     * - write/refresh remote TTL and start periodic refresh tasks
     */
    @InternalRedisAPI
    fun init(): Mono<Void>

    /**
     * Registers a listener that will be notified with change events of type [L].
     */
    fun addListener(listener: (L) -> Unit)

    /**
     * Removes a previously registered [listener].
     */
    fun removeListener(listener: (L) -> Unit)
}
