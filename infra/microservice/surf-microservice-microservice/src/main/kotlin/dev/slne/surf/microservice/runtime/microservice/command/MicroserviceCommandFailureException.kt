package dev.slne.surf.microservice.runtime.microservice.command

import java.io.Serial

internal class MicroserviceCommandFailureException(
    message: String
) : RuntimeException(message, null, false, false) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = 182560695059057715L
    }
}