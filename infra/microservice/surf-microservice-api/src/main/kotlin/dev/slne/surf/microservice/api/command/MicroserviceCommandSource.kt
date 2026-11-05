package dev.slne.surf.microservice.api.command

/**
 * Channel through which a command was invoked.
 */
enum class MicroserviceCommandSource {
    /**
     * Standard input of the process.
     */
    CONSOLE,

    /**
     * The local command socket, used by `microservicectl`.
     */
    LOCAL_SOCKET,
}
