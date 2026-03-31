package dev.slne.surf.redis.event

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.util.Initializable
import dev.slne.surf.redis.util.InternalRedisAPI
import kotlinx.coroutines.Deferred
import java.io.Closeable

/**
 * Redis-backed event bus based on Redis Pub/Sub.
 *
 * The event bus is responsible for:
 * - publishing [RedisEvent] instances to Redis (JSON-serialized)
 * - subscribing to the configured Redis event channel
 * - dispatching received events to locally registered handler methods
 *
 * Handler methods are discovered via [OnRedisEvent].
 *
 * ## Dispatch
 * Event handlers are invoked inside a coroutine launched on the internal Redis listener scope,
 * which runs on **[kotlinx.coroutines.Dispatchers.Default]** (see [OnRedisEvent]).
 * Both regular and `suspend` handler methods are supported.
 *
 * For invocation, the implementation generates **JVM hidden classes** at registration time.
 * Each hidden class wraps a `MethodHandle` as a `static final` constant, enabling the JIT
 * compiler to constant-fold and inline the dispatch target. This provides near-direct-call
 * performance while retaining the flexibility of reflection-based handler discovery.
 *
 * ## Lifecycle
 * The owning [RedisApi] initializes the bus during [RedisApi.connect] via [init] and closes it during
 * [RedisApi.disconnect].
 *
 * Listener registration is expected to happen before the [RedisApi] instance is frozen.
 */
interface RedisEventBus : Closeable, Initializable {

    /**
     * Publishes [event] to Redis asynchronously.
     *
     * The returned [Deferred] completes with the number of subscribers that received the message.
     * If the event could not be serialized, the deferred completes with `0`.
     *
     * @return a [Deferred] with the number of receiving subscribers, or `0` if serialization failed.
     */
    fun publish(event: RedisEvent): Deferred<Long>

    /**
     * Registers all event handler methods on the given [listener] instance.
     *
     * The listener is scanned for methods annotated with [OnRedisEvent].
     * Method requirements are defined by [OnRedisEvent].
     *
     * Implementations may reject registrations after the owning [RedisApi] is frozen.
     *
     * @throws IllegalStateException if registration is not allowed (e.g. after the owning [RedisApi] was frozen).
     */
    fun registerListener(listener: Any)
}