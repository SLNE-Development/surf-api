package dev.slne.surf.surfapi.core.api.command.exception

import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.slne.surf.surfapi.core.api.messages.ComponentMessage
import net.kyori.adventure.text.Component
import java.io.Serial

class WrapperCommandExceptionComponent(errorMessage: Component) : WrapperCommandSyntaxException(
    CommandSyntaxException(
        SimpleCommandExceptionType(
            ComponentMessage(errorMessage)
        ), ComponentMessage(errorMessage)
    )
) {
    override val message: String?
        get() = rawMessage.string

    companion object {
        @Serial
        private val serialVersionUID = -5277462249842682916L
    }
}
