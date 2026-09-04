package dev.slne.surf.api.minestom.command.commandapi.exception

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import java.io.Serial

open class CommandSyntaxException(
    val component: Component?,
    val input: String? = null,
    val cursor: Int? = null,
    cause: Throwable? = null,
) : RuntimeException(component?.let { PlainTextComponentSerializer.plainText().serialize(it) }, cause) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -540694787746386949L
    }
}

class WrapperCommandSyntaxException(
    val exception: CommandSyntaxException,
) : RuntimeException(exception.message, exception) {
    val component: Component?
        get() = exception.component

    val input: String?
        get() = exception.input

    val cursor: Int?
        get() = exception.cursor

    companion object {
        @Serial
        private const val serialVersionUID: Long = -2695063468338553356L
    }
}
