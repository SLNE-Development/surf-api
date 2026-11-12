package dev.slne.surf.microservice.api.command

import java.io.Serial

/**
 * Ends a command as failed.
 *
 * The caller receives [message] as an error line, without a stack trace. Throw it from
 * [MicroserviceCommand.execute], usually through [MicroserviceCommandContext.fail].
 *
 * @property message the failure message shown to the caller
 */
public class MicroserviceCommandException(
    override val message: String,
) : RuntimeException(message, null, false, false) {
    private companion object {
        @Serial
        private const val serialVersionUID: Long = 182560695059057715L
    }
}
