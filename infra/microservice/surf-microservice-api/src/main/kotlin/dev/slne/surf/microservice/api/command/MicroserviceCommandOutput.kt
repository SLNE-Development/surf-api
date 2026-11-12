package dev.slne.surf.microservice.api.command

import dev.slne.surf.microservice.api.InternalMicroserviceApi

/**
 * Receives the output of one command invocation.
 *
 * Implemented by the runtime: the console logs the lines, and the command socket forwards them to `microservicectl`
 * for each connection.
 */
@InternalMicroserviceApi
public interface MicroserviceCommandOutput {
    public fun sendLine(message: String)

    public fun sendError(message: String)
}
