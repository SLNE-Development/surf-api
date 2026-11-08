package dev.slne.surf.microservice.reliability.operation

import dev.slne.surf.nats.descriptor.NatsEvent

/**
 * An event a transactional handler published, held until its operation commits.
 *
 * @property aggregateId id of the entity the event is about; used in diagnostics only
 */
internal data class PendingEvent(val event: NatsEvent, val aggregateId: String?)
