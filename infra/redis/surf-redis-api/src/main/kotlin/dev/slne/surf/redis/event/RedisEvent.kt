package dev.slne.surf.redis.event

import dev.slne.surf.redis.RedisComponentProvider
import kotlinx.serialization.Serializable

/**
 * Base class for all Redis events.
 * Extend this class to create custom events that can be published and subscribed to.
 */
@Serializable
abstract class RedisEvent {
    /**
     * Timestamp when the event was created
     */
    val timestamp: Long = System.currentTimeMillis()

    /**
     * Identifier of the client that originally published this event.
     *
     * This value is set internally when the event is published by a Redis client.
     * It may be `null` if the event originates from an external source or if
     * the origin cannot be determined.
     */
    var originId: String? = null
        internal set

    /**
     * Returns `true` if this event was published by the current local Redis client.
     *
     * If [originId] is `null`, the event is considered to originate from
     * an external or unknown source and this method returns `false`.
     *
     * This is useful for filtering out self-originated events in distributed
     * or multi-client Redis environments.
     *
     * @return `true` if the event originated from this client, `false` otherwise
     */
    fun originatesFromThisClient(): Boolean {
        return originId == RedisComponentProvider.get().clientId
    }
}
