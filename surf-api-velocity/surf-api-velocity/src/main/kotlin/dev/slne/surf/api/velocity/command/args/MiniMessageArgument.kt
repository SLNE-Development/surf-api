package dev.slne.surf.api.velocity.command.args

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.velocitypowered.api.command.VelocityBrigadierMessage
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.CommandAPIArgumentType
import dev.jorel.commandapi.executors.CommandArguments
import dev.slne.surf.api.core.command.builder.CommandExceptionBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.ParsingException

open class MiniMessageArgument(nodeName: String) :
    Argument<Component>(nodeName, StringArgumentType::string) {
    override fun getPrimitiveType(): Class<Component> {
        return Component::class.java
    }

    override fun getArgumentType(): CommandAPIArgumentType? {
        return CommandAPIArgumentType.PRIMITIVE_TEXT
    }

    override fun <Source : Any?> parseArgument(
        cmdCtx: CommandContext<Source>,
        key: String,
        previousArgs: CommandArguments,
    ): Component {
        val raw = StringArgumentType.getString(cmdCtx, key)

        try {
            return MiniMessage.miniMessage().deserialize(raw)
        } catch (e: ParsingException) {
            throw SimpleCommandExceptionType(
                VelocityBrigadierMessage.tooltip(
                    CommandExceptionBuilder(
                        e.detailMessage(),
                        e.originalText(),
                        e.endIndex()
                    ).build()
                )
            ).create()
        }
    }
}

inline fun CommandAPICommand.miniMessageArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {},
): CommandAPICommand =
    withArguments(MiniMessageArgument(nodeName).setOptional(optional).apply(block))