package de.slne.redis.request

import kotlinx.serialization.Serializable

/**
 * Base class for all Redis responses.
 * Extend this class to create custom responses to requests.
 */
@Serializable
abstract class RedisResponse {
    /**
     * Timestamp when the response was created
     */
    val timestamp: Long = System.currentTimeMillis()
}
