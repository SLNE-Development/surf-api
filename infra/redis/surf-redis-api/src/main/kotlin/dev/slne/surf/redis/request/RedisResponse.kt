package dev.slne.surf.redis.request

import kotlinx.serialization.Serializable

/**
 * Base type for Redis response messages.
 *
 * Responses are published as replies to a corresponding [RedisRequest].
 * Since requests are broadcasted, **multiple responses may exist** for the same request.
 *
 * The sender side typically completes the request with the **first response**
 * that arrives; additional responses may be ignored.
 *
 * Implementations must be [kotlinx.serialization.Serializable] and should primarily
 * act as data carriers.
 */
@Serializable
abstract class RedisResponse {

    /**
     * Timestamp (epoch milliseconds) indicating when this response instance was created.
     *
     * This value is set locally at construction time and is intended for
     * debugging and tracing purposes.
     */
    val timestamp: Long = System.currentTimeMillis()
}
