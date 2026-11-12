package dev.slne.surf.microservice.runtime.command

import dev.slne.surf.microservice.api.command.MicroserviceCommandResult

/**
 * Outcome of [MicroserviceCommandDispatcher.dispatch].
 *
 * @property shutdownRequested whether the command asked for a shutdown. The caller requests it after delivering
 * [result], so the client still receives the response
 */
internal data class CommandDispatch(
    val result: MicroserviceCommandResult,
    val shutdownRequested: Boolean,
)
