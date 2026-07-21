package dev.slne.surf.microservice.api.microservice.command

/**
 * Represents the result of a microservice command invocation.
 */
sealed interface MicroserviceCommandResult {
    /**
     * The command completed successfully.
     */
    data object Success : MicroserviceCommandResult

    /**
     * The command could not be executed successfully.
     *
     * @property message A caller-safe description of the failure.
     */
    data class Failure(
        val message: String
    ) : MicroserviceCommandResult
}