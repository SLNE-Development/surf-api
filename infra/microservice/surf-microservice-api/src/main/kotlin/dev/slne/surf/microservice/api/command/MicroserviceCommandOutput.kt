package dev.slne.surf.microservice.api.command

import dev.slne.surf.microservice.api.InternalMicroserviceApi

/**
 * Receives the output of one command invocation.
 */
@InternalMicroserviceApi
interface MicroserviceCommandOutput {
    fun sendLine(message: String)

    fun sendError(message: String)
}
