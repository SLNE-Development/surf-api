@file:OptIn(ExperimentalVersionOverloading::class)

package dev.slne.surf.api.paper.command.args

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.GreedyStringArgument
import dev.jorel.commandapi.arguments.TextArgument
import dev.slne.surf.api.core.command.builder.CommandExceptionBuilder
import dev.slne.surf.api.core.minimessage.miniMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.ParsingException

open class MiniMessageArgument(
    nodeName: String,
    @IntroducedAt("3.32.0") greedy: Boolean = false,
) : CustomArgument<Component, String>(
    if (greedy) GreedyStringArgument(nodeName) else TextArgument(nodeName),
    { info ->
        val raw = info.currentInput

        try {
            miniMessage.deserialize(raw)
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
    @IntroducedAt("3.32.0") greedy: Boolean = false,
    block: Argument<*>.() -> Unit = {},
): CommandAPICommand = withArguments(MiniMessageArgument(nodeName, greedy).setOptional(optional).apply(block))

inline fun CommandTree.miniMessageArgument(
    nodeName: String,
    optional: Boolean = false,
    @IntroducedAt("3.32.0") greedy: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandTree = then(MiniMessageArgument(nodeName, greedy).setOptional(optional).apply(block))