package dev.slne.surf.redis.request

import java.io.Serial

/**
 * Thrown when a Redis request does not receive any response within the configured timeout.
 *
 * This exception indicates that no responding server published a matching
 * [RedisResponse] before the request timed out.
 */
class RequestTimeoutException(message: String) : Exception(message) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -8400201123891711077L
    }
}
