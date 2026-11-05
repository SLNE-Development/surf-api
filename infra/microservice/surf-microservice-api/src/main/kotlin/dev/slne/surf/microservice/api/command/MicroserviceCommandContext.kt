package dev.slne.surf.microservice.api.command

import dev.slne.surf.microservice.api.InternalMicroserviceApi
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * Execution environment of one command invocation.
 *
 * Child coroutines started in this scope belong to the invocation; the caller receives its response after they
 * complete.
 */
class MicroserviceCommandContext @InternalMicroserviceApi constructor(
    override val coroutineContext: CoroutineContext,
    val source: MicroserviceCommandSource,
    private val output: MicroserviceCommandOutput,
    private val shutdownRequest: () -> Unit,
) : CoroutineScope {

    /**
     * Sends a line to the caller.
     */
    fun sendLine(message: String) {
        output.sendLine(message)
    }

    /**
     * Sends an error line to the caller.
     */
    fun sendError(message: String) {
        output.sendError(message)
    }

    /**
     * Ends the command as failed with [message].
     */
    fun fail(message: String): Nothing = throw MicroserviceCommandException(message)

    /**
     * Requests a graceful shutdown once the response has been delivered.
     */
    fun requestShutdown() {
        shutdownRequest()
    }
}
