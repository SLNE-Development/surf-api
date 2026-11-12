package dev.slne.surf.microservice.reliability.test

import dev.slne.surf.nats.descriptor.NatsEvent

/**
 * An event that a committed block of [TransactionalHandlerFixture.runInHandler] published.
 *
 * @property event the event as passed to
 * [TransactionalEvents.publish][dev.slne.surf.microservice.reliability.TransactionalEvents.publish]
 * @property aggregateId the aggregate id passed to `publish`, or `null` if none was passed
 */
public data class PublishedTransactionalEvent(
    public val event: NatsEvent,
    public val aggregateId: String?,
)
