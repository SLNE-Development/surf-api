package dev.slne.surf.microservice.api.microservice.command

import dev.slne.surf.microservice.api.common.util.InternalMicroserviceApi
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

/**
 * Describes how a microservice command was invoked.
 */
enum class MicroserviceCommandSource {
    /**
     * The command was entered through the process standard input.
     */
    CONSOLE,

    /**
     * The command was submitted through the local command socket.
     */
    LOCAL_SOCKET
}

/**
 * Receives output produced by a microservice command.
 */
@InternalMicroserviceApi
interface MicroserviceCommandOutput {
    /**
     * Sends a regular output line to the command caller.
     */
    fun sendLine(message: String)

    /**
     * Sends an error output line to the command caller.
     */
    fun sendError(message: String)
}

/**
 * Provides the execution environment of a microservice command.
 *
 * The context is also a [CoroutineScope], allowing commands to start structured child coroutines.
 *
 * @property source The source from which the command was invoked.
 */
class MicroserviceCommandContext @InternalMicroserviceApi constructor(
    override val coroutineContext: CoroutineContext,
    val source: MicroserviceCommandSource,
    private val output: MicroserviceCommandOutput,
    private val shutdownRequest: () -> Unit
) : CoroutineScope {
    /**
     * Sends a regular output line to the command caller.
     */
    fun sendLine(message: String) {
        output.sendLine(message)
    }

    /**
     * Sends an error output line to the command caller.
     */
    fun sendError(message: String) {
        output.sendError(message)
    }

    /**
     * Requests a graceful shutdown after the current command response has been delivered.
     */
    fun requestShutdown() {
        shutdownRequest()
    }
}