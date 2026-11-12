package dev.slne.surf.microservice.api.command

/**
 * Outcome of a command invocation, as reported to the caller.
 */
public sealed interface MicroserviceCommandResult {
    /**
     * The command completed. `microservicectl` exits with code `0`.
     */
    public data object Success : MicroserviceCommandResult

    /**
     * The command failed. `microservicectl` exits with code `1`.
     *
     * @property message the failure message, safe to show to the caller
     */
    public data class Failure(public val message: String) : MicroserviceCommandResult
}
