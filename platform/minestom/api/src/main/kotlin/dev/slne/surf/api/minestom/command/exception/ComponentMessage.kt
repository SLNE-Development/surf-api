package dev.slne.surf.api.minestom.command.exception

import com.mojang.brigadier.Message
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer

/**
 * Brigadier [Message] that retains the [component] it was created from.
 *
 * [getString] returns the plain text of the component.
 */
class ComponentMessage(val component: Component) : Message {
    override fun getString(): String =
        PlainTextComponentSerializer.plainText().serialize(component)

    override fun toString(): String = string
}

/**
 * Returns the component this message was created from, or `null` if it is not a [ComponentMessage].
 */
fun Message.componentOrNull(): Component? = (this as? ComponentMessage)?.component
