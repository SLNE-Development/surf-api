package dev.slne.surf.redis.request

import dev.slne.surf.redis.RedisComponentProvider
import kotlinx.serialization.Serializable

/**
 * Base class for all Redis request messages.
 *
 * Subclasses represent requests that are sent via the request/response
 * mechanism and expect exactly one corresponding [RedisResponse].
 *
 * Requests must be serializable using Kotlin Serialization.
 */
@Serializable
abstract class RedisRequest {
    /**
     * Timestamp (milliseconds since epoch) when this request instance was created.
     *
     * This value is intended for debugging and tracing purposes.
     */
    val timestamp: Long = System.currentTimeMillis()

    /**
     * Identifier of the client that originally published this request.
     *
     * This value is set internally when the request is published by a Redis client.
     * It may be `null` if the request originates from an external source or if
     * the origin cannot be determined.
     */
    var originId: String? = null
        internal set

    /**
     * Returns `true` if this request was published by the current local Redis client.
     *
     * If [originId] is `null`, the request is considered to originate from an external
     * or unknown source and this method returns `false`.
     *
     * This is useful to ignore self-originated requests in a multi-client setup.
     */
    fun originatesFromThisClient(): Boolean {
        return originId == RedisComponentProvider.get().clientId
    }

}
