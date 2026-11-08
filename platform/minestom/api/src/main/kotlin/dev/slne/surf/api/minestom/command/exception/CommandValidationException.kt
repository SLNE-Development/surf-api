package dev.slne.surf.api.minestom.command.exception

import java.io.Serial

/**
 * Thrown when a command, argument or registration is configured invalidly, for example with a
 * malformed name, a required argument after an optional one or an unsupported suggestion provider.
 *
 * @param message description of the invalid configuration
 */
class CommandValidationException(message: String) : IllegalArgumentException(message) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -7547371556844925457L
    }
}
