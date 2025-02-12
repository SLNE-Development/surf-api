package dev.slne.surf.surfapi.core.api.command

import dev.slne.surf.surfapi.core.api.command.builder.CommandExceptionBuilder
import dev.slne.surf.surfapi.core.api.command.exception.WrapperCommandExceptionComponent
import net.kyori.adventure.text.ComponentLike

/**
 * Utility object for command-related operations and error handling in the Surf API.
 * Provides methods to create and throw custom command exceptions with user-friendly error messages.
 */
object SurfCommandUtil {

    /**
     * Creates a custom exception with the given message.
     *
     * @param message The message to include in the exception, as a [ComponentLike].
     * @return An instance of [WrapperCommandExceptionComponent].
     */
    @JvmStatic
    fun createException(message: ComponentLike) =
        WrapperCommandExceptionComponent(message.asComponent())

    /**
     * Throws a custom command exception with the given message.
     *
     * @param message The message to include in the exception, as a [ComponentLike].
     * @throws WrapperCommandExceptionComponent Always throws the created exception.
     */
    @JvmStatic
    fun failWithMessage(message: ComponentLike) {
        throw createException(message)
    }

    /**
     * Throws a custom command exception using a provided [CommandExceptionBuilder].
     *
     * @param builder The builder used to construct the exception message.
     * @throws WrapperCommandExceptionComponent Always throws the created exception.
     */
    @JvmStatic
    fun failWithBuilder(builder: CommandExceptionBuilder) {
        throw createException(builder.build())
    }
}
