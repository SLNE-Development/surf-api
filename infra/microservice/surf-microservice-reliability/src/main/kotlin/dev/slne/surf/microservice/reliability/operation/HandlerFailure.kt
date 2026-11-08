package dev.slne.surf.microservice.reliability.operation

import java.io.Serial

/**
 * Carries an exception of the handler through `AtomicOperations.execute`, so it can be told apart from failures of
 * the claim, the reply store or the commit.
 */
internal class HandlerFailure(
    cause: Exception,
) : RuntimeException(cause.message, cause, false, false) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 2830983045242379748L
    }
}
