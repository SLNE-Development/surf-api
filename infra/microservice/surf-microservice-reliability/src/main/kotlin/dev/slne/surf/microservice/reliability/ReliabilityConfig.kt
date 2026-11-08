package dev.slne.surf.microservice.reliability

import dev.slne.surf.database.reliability.operation.AtomicOperationsConfig
import dev.slne.surf.nats.rpc.NatsPersistentLimits
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Settings of the idempotent handlers of a microservice.
 *
 * @property atomicOperations Settings of the stored replies. The default retention is
 * [NatsPersistentLimits.REDELIVERY_HORIZON], the longest time after which a message with the same identity can reach
 * a handler; a shorter retention lets a late redelivery or dead-letter replay run the handler again.
 * @property cleanupInterval How often expired stored replies of all services on the database are deleted.
 * @throws IllegalArgumentException if [cleanupInterval] is not positive
 */
data class ReliabilityConfig(
    val atomicOperations: AtomicOperationsConfig = AtomicOperationsConfig(
        retention = NatsPersistentLimits.REDELIVERY_HORIZON,
    ),
    val cleanupInterval: Duration = 1.hours,
) {
    init {
        require(cleanupInterval.isPositive()) { "cleanupInterval must be positive, was $cleanupInterval" }
    }
}
