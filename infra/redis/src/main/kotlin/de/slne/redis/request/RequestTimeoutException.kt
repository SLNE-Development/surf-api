package de.slne.redis.request

import java.io.Serial

/**
 * Thrown when a request does not receive a response within the configured timeout.
 */
class RequestTimeoutException(message: String) : Exception(message) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -8400201123891711077L
    }
}
