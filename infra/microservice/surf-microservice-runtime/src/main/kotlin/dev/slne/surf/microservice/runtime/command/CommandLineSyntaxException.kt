package dev.slne.surf.microservice.runtime.command

import java.io.Serial

internal class CommandLineSyntaxException(message: String) : IllegalArgumentException(message) {
    private companion object {
        @Serial
        private const val serialVersionUID: Long = -2056254127114502369L
    }
}
