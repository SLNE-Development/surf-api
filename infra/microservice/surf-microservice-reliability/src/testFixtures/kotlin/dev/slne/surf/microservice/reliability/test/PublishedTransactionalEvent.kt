package dev.slne.surf.microservice.reliability.test

import dev.slne.surf.nats.descriptor.NatsEvent

/**
 * An event that a committed block of [TransactionalHandlerFixture.runInHandler] published.
 *
 * @property event the event as passed to [TransactionalEvents.publish][dev.slne.surf.microservice.reliability.TransactionalEvents.publish]
 * @property aggregateId the aggregate id passed to `publish`
 */
data class PublishedTransactionalEvent(
    val event: NatsEvent,
    val aggregateId: String?,
)
