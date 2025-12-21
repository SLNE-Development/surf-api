package de.slne.redis.request

/**
 * Exception thrown when a request times out without receiving a response.
 */
class RequestTimeoutException(message: String) : Exception(message)
