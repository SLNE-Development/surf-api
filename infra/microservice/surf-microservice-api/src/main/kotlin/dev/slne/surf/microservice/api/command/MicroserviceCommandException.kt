package dev.slne.surf.microservice.api.command

import java.io.Serial

/**
 * Ends a command as failed; the caller receives [message] without a stack trace.
 */
class MicroserviceCommandException(
    override val message: String,
) : RuntimeException(message, null, false, false) {
    private companion object {
        @Serial
        private const val serialVersionUID: Long = 182560695059057715L
    }
}
