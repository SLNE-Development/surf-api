package dev.slne.surf.microservice.reliability

import dev.slne.surf.database.reliability.operation.AtomicOperationsConfig
import dev.slne.surf.nats.rpc.NatsPersistentLimits
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Settings of the idempotent handlers of a microservice.
 *
 * Provide them through [ReliabilityCustomizer].
 *
 * @property atomicOperations the settings of the stored replies, such as their retention and the lock timeout. The
 * default retention is [NatsPersistentLimits.REDELIVERY_HORIZON], the longest time after which a message with the
 * same identity can reach a handler. A shorter retention lets a late redelivery or dead-letter replay run the handler
 * again.
 * @property cleanupInterval how often the expired stored replies of all services on the database are deleted. The
 * first cleanup runs one interval after the start.
 * @throws IllegalArgumentException if [cleanupInterval] is not positive
 */
public data class ReliabilityConfig(
    public val atomicOperations: AtomicOperationsConfig = AtomicOperationsConfig(
        retention = NatsPersistentLimits.REDELIVERY_HORIZON,
    ),
    public val cleanupInterval: Duration = 1.hours,
) {
    init {
        require(cleanupInterval.isPositive()) { "cleanupInterval must be positive, was $cleanupInterval" }
    }
}
