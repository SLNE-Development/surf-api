package dev.slne.surf.microservice.api.command

/**
 * Channel through which a command was invoked.
 */
public enum class MicroserviceCommandSource {
    /**
     * The standard input of the process. Output goes to the log.
     */
    CONSOLE,

    /**
     * The local command socket, used by `microservicectl`. Output goes to the standard output and standard error of
     * `microservicectl`.
     */
    LOCAL_SOCKET,
}
