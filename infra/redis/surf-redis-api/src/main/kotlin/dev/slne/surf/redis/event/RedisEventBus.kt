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
 * Event handlers are invoked synchronously on a Redisson/Reactor thread (see [OnRedisEvent]).
 *
 * For invocation, the implementation uses `MethodHandle`s (via `java.lang.invoke`) rather than
 * JVM-generated lambda factories to avoid classloader-related issues.
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