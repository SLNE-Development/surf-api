package dev.slne.surf.redis.event

import dev.slne.surf.redis.RedisComponentProvider
import kotlinx.serialization.Serializable

/**
 * Base type for events distributed via Redis.
 *
 * All events that are published through the Redis event bus must extend this class.
 * Events are serialized and propagated to other Redis-connected clients.
 *
 * Implementations are expected to be simple data holders. Events using the default JSON transport
 * must be [kotlinx.serialization.Serializable]. Events with a registered [RedisEventCodec] may use
 * that codec instead and are sent on the isolated binary event channel.
 */
@Serializable
abstract class RedisEvent {
    /**
     * Timestamp (epoch milliseconds) indicating when this event instance was created.
     *
     * This value is set locally at construction time.
     */
    var timestamp: Long = System.currentTimeMillis()
        internal set

    /**
     * Identifier of the client that originally published this event.
     *
     * This field is assigned internally when the event is published via the Redis API.
     * It may be `null` if the event originates from an external source or if the origin
     * cannot be determined.
     */
    var originId: String? = null
        internal set

    /**
     * Indicates whether this event was published by the current local Redis client.
     *
     * If [originId] is `null`, the event is treated as originating from an
     * external or unknown source and this method returns `false`.
     *
     * This method is typically used to ignore self-originated events in
     * multi-client or distributed environments.
     *
     * @return `true` if the event originated from this client, `false` otherwise.
     */
    fun originatesFromThisClient(): Boolean {
        return originId == RedisComponentProvider.clientId
    }
}
