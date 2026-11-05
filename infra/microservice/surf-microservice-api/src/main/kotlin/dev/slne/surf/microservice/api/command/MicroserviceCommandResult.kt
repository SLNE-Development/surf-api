package dev.slne.surf.microservice.api.command

/**
 * Outcome of a command invocation.
 */
sealed interface MicroserviceCommandResult {
    /**
     * The command completed.
     */
    data object Success : MicroserviceCommandResult

    /**
     * The command failed; [message] is safe to show to the caller.
     */
    data class Failure(val message: String) : MicroserviceCommandResult
}
