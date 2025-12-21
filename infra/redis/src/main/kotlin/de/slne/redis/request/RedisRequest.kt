package de.slne.redis.request

import kotlinx.serialization.Serializable

/**
 * Base class for all Redis requests.
 * Extend this class to create custom requests that expect a response.
 */
@Serializable
abstract class RedisRequest {
    /**
     * Timestamp when the request was created
     */
    val timestamp: Long = System.currentTimeMillis()
}
