package dev.slne.surf.api.minestom.command.commandapi.exception

import com.mojang.brigadier.Message
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/**
 * A Brigadier [Message] that keeps the component it was built from.
 */
class ComponentMessage(val component: Component) : Message {
    override fun getString(): String =
        PlainTextComponentSerializer.plainText().serialize(component)

    override fun toString(): String = string
}

/** The component [Message] was built from, or `null` if it carries none. */
fun Message.componentOrNull(): Component? = (this as? ComponentMessage)?.component
