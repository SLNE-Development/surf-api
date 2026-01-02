package dev.slne.surf.redis.event

import dev.slne.surf.redis.RedisApi
import dev.slne.surf.redis.util.InternalRedisAPI
import kotlinx.coroutines.Deferred
import java.io.Closeable

/**
 * Redis-backed event bus based on Redis Pub/Sub.
 *
 * This event bus:
 * - Publishes events to Redis using JSON serialization
 * - Subscribes to a single Redis channel
 * - Dispatches events to locally registered handlers
 *
 * Event handlers are discovered via the [OnRedisEvent] annotation
 * and are invoked using JVM-generated lambdas for minimal dispatch overhead.
 *
 * Registration must happen before the owning [dev.slne.surf.redis.RedisApi] is frozen.
 */
interface RedisEventBus : Closeable {

    /**
     * Initializes the event bus by subscribing to the Redis event channel.
     *
     * This method must be called during startup.
     */
    @InternalRedisAPI
    fun init()

    /**
     * Publishes an event to Redis asynchronously.
     *
     * @return a [kotlinx.coroutines.Deferred] containing the number of receiving subscribers,
     *         or `0` if the event could not be serialized
     */
    fun publish(event: RedisEvent): Deferred<Long>

    /**
     * Registers all event handler methods on the given listener instance.
     *
     * Methods annotated with [OnRedisEvent] must:
     * - Have exactly one parameter
     * - Accept a subtype of [RedisEvent]
     *
     * @throws IllegalStateException if the [RedisApi] has already been frozen
     */
    fun registerListener(listener: Any)
}