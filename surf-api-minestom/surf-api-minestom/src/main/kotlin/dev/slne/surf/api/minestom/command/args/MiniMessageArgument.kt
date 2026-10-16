package dev.slne.surf.api.minestom.command.args

import dev.slne.surf.api.core.command.builder.CommandExceptionBuilder
import dev.slne.surf.api.core.minimessage.miniMessage
import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.CommandTree
import dev.slne.surf.api.minestom.command.argument.Argument
import dev.slne.surf.api.minestom.command.argument.CustomArgument
import dev.slne.surf.api.minestom.command.argument.GreedyStringArgument
import dev.slne.surf.api.minestom.command.argument.TextArgument
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.ParsingException

/**
 * Parses a MiniMessage string into a [Component].
 *
 * Malformed MiniMessage fails the command with the parser's detail message and error position.
 *
 * @param greedy whether the argument consumes the rest of the command line instead of one quoted or
 * unquoted text token
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
