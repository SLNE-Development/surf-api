package dev.slne.surf.api.minestom.command.args

import dev.slne.minestom.lobby.api.command.commandapi.CommandAPI
import dev.slne.minestom.lobby.api.command.commandapi.CommandAPICommand
import dev.slne.minestom.lobby.api.command.commandapi.CommandTree
import dev.slne.minestom.lobby.api.command.commandapi.argument.Argument
import dev.slne.minestom.lobby.api.command.commandapi.argument.CustomArgument
import dev.slne.minestom.lobby.api.command.commandapi.argument.GreedyStringArgument
import dev.slne.minestom.lobby.api.command.commandapi.argument.TextArgument
import dev.slne.surf.api.core.command.builder.CommandExceptionBuilder
import dev.slne.surf.api.core.minimessage.miniMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.ParsingException

/**
 * Reads a component written in MiniMessage.
 *
 * @param greedy whether the argument consumes the rest of the command line
 */
open class MiniMessageArgument(
    nodeName: String,
    greedy: Boolean = false,
) : CustomArgument<Component, String>(
    if (greedy) GreedyStringArgument(nodeName) else TextArgument(nodeName),
    { info ->
        try {
            miniMessage.deserialize(info.currentInput)
        } catch (e: ParsingException) {
            CommandAPI.failWithMessage(
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
    greedy: Boolean = false,
    block: Argument<Component>.() -> Unit = {},
): CommandAPICommand =
    withArguments(MiniMessageArgument(nodeName, greedy).setOptional(optional).apply(block))

inline fun CommandTree.miniMessageArgument(
    nodeName: String,
    optional: Boolean = false,
    greedy: Boolean = false,
    block: Argument<Component>.() -> Unit = {},
): CommandTree = then(MiniMessageArgument(nodeName, greedy).setOptional(optional).apply(block))

inline fun <T> Argument<T>.miniMessageArgument(
    nodeName: String,
    optional: Boolean = false,
    greedy: Boolean = false,
    block: Argument<Component>.() -> Unit = {},
): Argument<T> = then(MiniMessageArgument(nodeName, greedy).setOptional(optional).apply(block))
