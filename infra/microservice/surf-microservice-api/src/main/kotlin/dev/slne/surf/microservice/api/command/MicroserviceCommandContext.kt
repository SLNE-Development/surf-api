package dev.slne.surf.microservice.api.command

import dev.slne.surf.microservice.api.InternalMicroserviceApi
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * Execution environment of one command invocation.
 *
 * The context is a [CoroutineScope]. Child coroutines started in it belong to the invocation, and the caller receives
 * the response only after they complete.
 *
 * @property source the channel through which the command was invoked
 */
public class MicroserviceCommandContext @InternalMicroserviceApi constructor(
    override val coroutineContext: CoroutineContext,
    public val source: MicroserviceCommandSource,
    private val output: MicroserviceCommandOutput,
    private val shutdownRequest: () -> Unit,
) : CoroutineScope {

    /**
     * Sends a line to the caller.
     *
     * On the console the line is logged at info level. Through `microservicectl` it is written to standard output.
     */
    public fun sendLine(message: String) {
        output.sendLine(message)
    }

    /**
     * Sends an error line to the caller without ending the command.
     *
     * On the console the line is logged at warning level. Through `microservicectl` it is written to standard error.
     * Use [fail] to end the command as failed.
     */
    public fun sendError(message: String) {
        output.sendError(message)
    }

    /**
     * Ends the command as failed with [message] by throwing a [MicroserviceCommandException].
     *
     * The caller receives [message] without a stack trace, and `microservicectl` exits with code `1`.
     */
    public fun fail(message: String): Nothing = throw MicroserviceCommandException(message)

    /**
     * Requests a graceful shutdown of the microservice.
     *
     * The shutdown begins after the command completed and its response was delivered. The command itself keeps
     * running until it returns.
     */
    public fun requestShutdown() {
        shutdownRequest()
    }
}
