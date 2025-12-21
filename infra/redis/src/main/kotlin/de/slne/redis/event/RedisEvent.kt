package de.slne.redis.event

/**
 * Base class for all Redis events.
 * Extend this class to create custom events that can be published and subscribed to.
 */
abstract class RedisEvent {
    /**
     * Timestamp when the event was created
     */
    val timestamp: Long = System.currentTimeMillis()
}
