package dev.slne.surf.api.minestom.command.commandapi.exception

import java.io.Serial

/**
 * Exception thrown to indicate that a command validation error has occurred.
 *
 * This exception is used to signal issues related to the validation
 * of command inputs, parameters, or definitions within the Command API.
 *
 * @param message A detailed message describing the validation error.
 */
class CommandValidationException(message: String) : IllegalArgumentException(message) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -7547371556844925457L
    }
}
