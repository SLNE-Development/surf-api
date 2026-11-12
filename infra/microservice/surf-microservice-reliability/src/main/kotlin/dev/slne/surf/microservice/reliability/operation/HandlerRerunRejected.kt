package dev.slne.surf.microservice.reliability.operation

import java.io.Serial

/**
 * Stops a retry of `AtomicOperations.execute` that would run the handler a second time for one delivery.
 *
 * A delivery can run its handler only once, so the retry becomes a redelivery of the message instead.
 */
internal class HandlerRerunRejected : RuntimeException(
    "The transaction failed after the handler ran and must be retried with a new delivery",
    null,
    false,
    false,
) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 2698411133330069057L
    }
}
