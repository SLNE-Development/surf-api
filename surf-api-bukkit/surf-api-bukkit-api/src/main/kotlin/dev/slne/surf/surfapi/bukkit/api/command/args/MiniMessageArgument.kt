package dev.slne.surf.surfapi.bukkit.api.command.args

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.slne.surf.surfapi.core.api.command.builder.CommandExceptionBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.ParsingException

open class MiniMessageArgument(nodeName: String) : CustomArgument<Component, String>(
    TextArgument(nodeName),
    { info ->
        val raw = info.currentInput

        try {
            MiniMessage.miniMessage().deserialize(raw)
        } catch (e: ParsingException) {
            throw CustomArgumentException.fromAdventureComponent(
                CommandExceptionBuilder(
                    e.detailMessage(),
                    e.originalText(),
                    e.endIndex()
                ).build()
            )
        }
    }
)

inline fun CommandAPICommand.miniMessageArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {},
): CommandAPICommand =
    withArguments(MiniMessageArgument(nodeName).setOptional(optional).apply(block))