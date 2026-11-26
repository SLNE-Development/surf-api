package dev.slne.surf.api.command.exception

import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import net.kyori.adventure.text.Component
import java.io.Serial

/**
 * A wrapper for [CommandSyntaxException] to integrate custom error messages
 * as [Component] objects within the Surf API.
 *
 * The command dispatcher of the platform renders the message of the exception as [component].
 * Creating the exception needs surf-api's root context with a [CommandMessageFactory] bean, which
 * Paper and Velocity provide.
 *
 * @param component The error message to associate with the exception.
 * @throws IllegalStateException if the root context is not installed yet.
 * @throws org.springframework.beans.factory.NoSuchBeanDefinitionException if the platform provides
 * no [CommandMessageFactory].
 */
class WrapperCommandExceptionComponent(
    val component: Component,
) : WrapperCommandSyntaxException(createSyntaxException(component)) {

    override val message: String?
        get() = rawMessage.string

    companion object {
        @Serial
        private val serialVersionUID = -5277462249842682916L
    }
}

private fun createSyntaxException(component: Component): CommandSyntaxException {
    val message = CommandMessageFactory.instance.create(component)

    return CommandSyntaxException(SimpleCommandExceptionType(message), message)
}
