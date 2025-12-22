package de.slne.redis.request

import kotlinx.serialization.Serializable

/**
 * Base class for all Redis response messages.
 *
 * Subclasses represent responses sent for a corresponding [RedisRequest].
 * Each request may produce at most one response.
 *
 * Responses must be serializable using Kotlin Serialization.
 */
@Serializable
abstract class RedisResponse {
    /**
     * Timestamp (milliseconds since epoch) when this response instance was created.
     *
     * This value is intended for debugging and tracing purposes.
     */
    val timestamp: Long = System.currentTimeMillis()
}
