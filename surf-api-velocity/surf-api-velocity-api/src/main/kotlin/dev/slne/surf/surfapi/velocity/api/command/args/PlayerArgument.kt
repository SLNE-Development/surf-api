package dev.slne.surf.surfapi.velocity.api.command.args

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.proxy.Player
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CommandAPIArgumentType
import dev.jorel.commandapi.executors.CommandArguments

class PlayerArgument(nodeName: String) : Argument<Player>(nodeName, StringArgumentType.word()) {
    companion object {
        private val NO_PLAYERS_FOUND = SimpleCommandExceptionType("No player was found")
    }

    init {
        replaceSuggestions(ArgumentSuggestions.stringCollection {
            InternalCommandBridge.getPlayers().map { it.username }
        })
    }

    override fun getPrimitiveType(): Class<Player> {
        return Player::class.java
    }

    override fun getArgumentType(): CommandAPIArgumentType? {
        return CommandAPIArgumentType.PRIMITIVE_STRING
    }

    override fun <Source : Any> parseArgument(
        cmdCtx: CommandContext<Source>,
        key: String,
        previousArgs: CommandArguments,
    ): Player {
        val playerName = StringArgumentType.getString(cmdCtx, key)
        return InternalCommandBridge.getPlayer(playerName) ?: throw NO_PLAYERS_FOUND.create()
    }
}

inline fun CommandAPICommand.playerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {},
): CommandAPICommand =
    withArguments(PlayerArgument(nodeName).setOptional(optional).apply(block))