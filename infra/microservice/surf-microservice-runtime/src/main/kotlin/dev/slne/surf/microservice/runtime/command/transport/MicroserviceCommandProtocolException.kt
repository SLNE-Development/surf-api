package dev.slne.surf.microservice.runtime.command.transport

import java.io.IOException
import java.io.Serial

internal class MicroserviceCommandProtocolException(message: String) : IOException(message) {
    private companion object {
        @Serial
        private const val serialVersionUID: Long = -166141481579158655L
    }
}
