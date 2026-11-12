package dev.slne.surf.microservice.reliability

import dev.slne.surf.microservice.reliability.operation.ActiveOperation
import dev.slne.surf.microservice.reliability.operation.PendingEvent
import dev.slne.surf.nats.descriptor.NatsEvent
import kotlinx.coroutines.currentCoroutineContext

/**
 * Publishes the events of transactional handlers after their transaction commits.
 *
 * Get the publisher of the running microservice with [Microservice.events][events], or from
 * `TransactionalHandlerFixture.events` in tests, and pass it to the service implementation.
 *
 * [publish] works only in a handler of a service registered with [registerTransactionalService], including child
 * coroutines the handler starts. The events are held in memory and, once the handler's transaction has committed,
 * published in publish order with [NatsClient.publish][dev.slne.surf.nats.NatsClient.publish] on the client the
 * service is registered on. If the handler fails or its transaction rolls back, they are discarded. A redelivered
 * message whose operation already committed does not run the handler and publishes nothing.
 *
 * Delivery is best effort, like every core NATS event. If the instance stops between the commit and the publish, or a
 * publish fails, the event is lost and the failure is logged. There is no retry. Each event gets a new random
 * [eventId][dev.slne.surf.nats.event.NatsEventContext.eventId]. Its origin is the origin of the client the service is
 * registered on, so that client's listeners see the event as their own.
 */
public class TransactionalEvents internal constructor() {

    /**
     * Publishes [event] after the transaction of the running handler commits.
     *
     * Returns immediately. The event is not validated here: an event that is no generated event message or exceeds
     * the server's payload limit fails to publish after the commit, which is logged.
     *
     * @param event the event to publish
     * @param aggregateId the id of the entity the event is about, or `null`. Appears in the log if the publish fails
     * @throws IllegalStateException if the caller does not run in a transactional handler, or runs after the handler
     * returned
     */
    public suspend fun publish(event: NatsEvent, aggregateId: String? = null) {
        val operation = checkNotNull(currentCoroutineContext()[ActiveOperation]) {
            "Transactional events can only be published in a handler of a service registered with " +
                "registerTransactionalService; use NatsClient.publish outside of it"
        }
        operation.add(PendingEvent(event, aggregateId))
    }
}
