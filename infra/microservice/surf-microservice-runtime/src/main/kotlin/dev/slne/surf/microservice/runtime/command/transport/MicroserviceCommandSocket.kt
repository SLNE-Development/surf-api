package dev.slne.surf.microservice.runtime.command.transport

import dev.slne.surf.api.core.environment.env
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Location of the command socket shared with `microservicectl`.
 */
internal object MicroserviceCommandSocket {
    const val PATH_ENVIRONMENT_VARIABLE = "SURF_MICROSERVICE_COMMAND_SOCKET"
    private const val DEFAULT_PATH = "/tmp/surf-microservice.sock"

    /**
     * Returns the socket path from [PATH_ENVIRONMENT_VARIABLE], or the default path both sides agree on.
     */
    fun resolvePath(): Path {
        val configuredPath = env.optional(PATH_ENVIRONMENT_VARIABLE, Path::of) ?: Path(DEFAULT_PATH)
        return configuredPath.toAbsolutePath().normalize()
    }
}
