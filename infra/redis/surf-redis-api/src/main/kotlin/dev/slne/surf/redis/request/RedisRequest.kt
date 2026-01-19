package dev.slne.surf.redis.request

import dev.slne.surf.redis.RedisComponentProvider
import kotlinx.serialization.Serializable

/**
 * Base type for Redis request messages.
 *
 * Requests are sent via the Redis request/response mechanism using Redis Pub/Sub.
 * A request is broadcasted, so **multiple servers/handlers may respond**.
 *
 * On the sender side, the request is typically completed with the **first response**
 * that arrives; additional responses may be ignored.
 *
 * Implementations must be [kotlinx.serialization.Serializable] and should primarily
 * act as data carriers.
 */
@Serializable
abstract class RedisRequest {
    /**
     * Timestamp (epoch milliseconds) indicating when this request instance was created.
     *
     * This value is set locally at construction time and is intended for
     * debugging and tracing purposes.
     */
    val timestamp: Long = System.currentTimeMillis()

    /**
     * Identifier of the client that originally sent this request.
     *
     * This field is assigned internally when the request is sent via the Redis API.
     * It may be `null` if the origin cannot be determined or if the request
     * originates from an external source.
     */
    var originId: String? = null
        internal set

    /**
     * Indicates whether this request was sent by the current local Redis client.
     *
     * If [originId] is `null`, the request is treated as originating from an
     * external or unknown source and this method returns `false`.
     *
     * @return `true` if the request originated from this client, `false` otherwise.
     */
    fun originatesFromThisClient(): Boolean {
        return originId == RedisComponentProvider.get().clientId
    }

}
