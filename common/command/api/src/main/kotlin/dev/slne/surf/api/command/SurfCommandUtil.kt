package dev.slne.surf.api.command

import dev.slne.surf.api.command.builder.CommandExceptionBuilder
import dev.slne.surf.api.command.exception.WrapperCommandExceptionComponent
import net.kyori.adventure.text.ComponentLike

/**
 * Creates and throws command exceptions that carry a user-facing message.
 */
object SurfCommandUtil {

    /**
     * Creates a custom exception with the given message.
     *
     * @param message The message to include in the exception, as a [ComponentLike].
     * @return An instance of [WrapperCommandExceptionComponent].
     */
    fun createException(message: ComponentLike): WrapperCommandExceptionComponent {
        return WrapperCommandExceptionComponent(message.asComponent())
    }

    /**
     * Aborts the running command with an exception that carries [message].
     *
     * Never returns, so the call ends a branch in Kotlin.
     *
     * @param message The message to show to the command sender.
     * @throws WrapperCommandExceptionComponent Always.
     */
    fun failWithMessage(message: ComponentLike): Nothing = throw createException(message)

    /**
     * Aborts the running command with the exception that [builder] builds.
     *
     * Never returns, so the call ends a branch in Kotlin.
     *
     * @param builder The builder of the exception message.
     * @throws WrapperCommandExceptionComponent Always.
     */
    fun failWithBuilder(builder: CommandExceptionBuilder): Nothing {
        throw createException(builder.build())
    }
}
