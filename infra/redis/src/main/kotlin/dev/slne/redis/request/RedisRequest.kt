package dev.slne.redis.request

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
}
