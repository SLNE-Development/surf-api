package de.slne.redis.sync

import de.slne.redis.RedisApi
import dev.slne.surf.surfapi.core.api.util.logger
import io.lettuce.core.pubsub.RedisPubSubListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import org.jetbrains.annotations.MustBeInvokedByOverriders
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Base class for replicated, in-memory synchronized data structures.
 *
 * A [SyncStructure] provides the common plumbing for:
 * - **Snapshot bootstrap** for late joiners via [loadSnapshot]
 * - **Delta replication** via Redis Pub/Sub ([redisChannel] + [handleIncoming])
 * - **Non-blocking Redis I/O** (uses Lettuce async APIs)
 * - **Thread-safe local state** via [lock]
 *
 * ## Lifecycle
 * Implementations are expected to call [init] once during startup:
 * 1. [loadSnapshot] is executed to populate local state (or defaults).
 * 2. The instance subscribes to [redisChannel] and starts receiving deltas.
 *
 * The class itself does not start heartbeats or TTL refreshes; those belong to the concrete structure.
 *
 * ## Threading model
 * - Incoming Pub/Sub messages are delivered on Lettuce's Pub/Sub thread.
 * - [handleIncoming] is called directly from that thread (i.e. it must be fast and non-blocking).
 * - If heavier work is required, implementations should offload to [scope] (e.g. `scope.launch { ... }`).
 *
 * ## Concurrency
 * - Use [lock] to protect reads/writes of the local in-memory structure.
 * - Prefer small critical sections to avoid blocking other readers/writers.
 *
 * @param TDelta the delta type (wire-level operation) used by the concrete structure
 * @param api owning [RedisApi] instance used for Redis connections
 * @param id logical identifier of the structure (typically part of Redis keys/channel names)
 * @param scope coroutine scope used for asynchronous work (e.g. resync, publishing deltas)
 */
abstract class SyncStructure<TDelta : Any> internal constructor(
    protected val api: RedisApi,
    protected val id: String,
    protected val scope: CoroutineScope
) {
    /**
     * Read-write lock guarding the local in-memory state of the structure.
     *
     * Implementations should use `lock.readLock()/writeLock()` or Kotlin extensions
     * such as `kotlin.concurrent.read` / `kotlin.concurrent.write`.
     */
    protected val lock = ReentrantReadWriteLock()

    /**
     * Redis Pub/Sub channel used to broadcast and receive deltas for this structure instance.
     */
    protected abstract val redisChannel: String

    companion object {
        private val log = logger()
    }

    /**
     * Initializes the structure by loading the initial snapshot and subscribing to deltas.
     *
     * This method is `internal` to enforce that the owning API/module controls initialization order
     * (e.g. after Redis connections are available and before freeze/registration steps are finalized).
     *
     * Implementations may override this method to extend initialization (e.g. start heartbeats),
     * but should call `super.init()`.
     */
    @MustBeInvokedByOverriders
    internal open suspend fun init() {
        loadSnapshot()
        setupSubscription()
    }

    /**
     * Subscribes to [redisChannel] and installs the Pub/Sub listener.
     *
     * Incoming messages for this channel are forwarded to [handleIncoming].
     */
    private suspend fun setupSubscription() {
        api.pubSubConnection.addListener(object : RedisPubSubListener<String, String> {
            override fun message(channel: String, message: String) {
                if (channel == redisChannel) {
                    handleIncoming(message)
                }
            }

            override fun message(pattern: String, channel: String, message: String) {}
            override fun subscribed(channel: String, count: Long) {}
            override fun psubscribed(pattern: String, count: Long) {}
            override fun unsubscribed(channel: String, count: Long) {}
            override fun punsubscribed(pattern: String, count: Long) {}
        })

        api.pubSubConnection.async().subscribe(redisChannel).await()
    }

    /**
     * Loads the current snapshot from Redis and applies it to local state.
     *
     * Used for initial bootstrapping and for recovery when deltas were missed.
     *
     * Implementations should:
     * - fetch their snapshot/version keys asynchronously,
     * - decode the snapshot,
     * - update local state under [lock].
     */
    protected abstract suspend fun loadSnapshot()

    /**
     * Handles a delta message received via Pub/Sub.
     *
     * This method is called on the Redis Pub/Sub thread and must not block.
     * Implementations should parse the message quickly and:
     * - apply the delta under [lock], or
     * - schedule heavier recovery work on [scope] (e.g. `scope.launch { loadSnapshot() }`).
     */
    protected abstract fun handleIncoming(message: String)

    /**
     * Utility to notify listeners safely.
     *
     * Exceptions thrown by listeners are caught and logged so that one faulty listener
     * does not break replication/dispatch.
     *
     * @param listeners list of listeners to call
     * @param change change object to pass to listeners
     */
    protected fun <T> notifyListeners(listeners: List<(T) -> Unit>, change: T) {
        for (l in listeners) {
            try {
                l(change)
            } catch (e: Throwable) {
                log.atWarning()
                    .withCause(e)
                    .log("Error notifying listener of change: $change")
            }
        }
    }
}
