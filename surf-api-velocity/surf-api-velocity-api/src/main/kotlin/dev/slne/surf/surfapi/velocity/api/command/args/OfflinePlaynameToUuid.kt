package dev.slne.surf.surfapi.velocity.api.command.args

import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CommandAPIArgumentType
import dev.jorel.commandapi.executors.CommandArguments
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.util.*

class OfflinePlaynameToUuid(nodeName: String) :
    Argument<Deferred<UUID?>>(nodeName, StringArgumentType.word()) {

        init {
            replaceSuggestions(ArgumentSuggestions.stringCollection {
                InternalCommandBridge.getPlayers().map { it.username }
            })
        }

    override fun getPrimitiveType(): Class<Deferred<UUID?>> {
        @Suppress("UNCHECKED_CAST")
        return Deferred::class.java as Class<Deferred<UUID?>>
    }

    override fun getArgumentType(): CommandAPIArgumentType? {
        return CommandAPIArgumentType.PRIMITIVE_STRING
    }

    override fun <Source : Any> parseArgument(
        cmdCtx: CommandContext<Source>,
        key: String,
        previousArgs: CommandArguments,
    ): Deferred<UUID?> {
        val playerName = StringArgumentType.getString(cmdCtx, key)
        val onlinePlayer = InternalCommandBridge.getPlayer(playerName)

        if (onlinePlayer != null) {
            return CompletableDeferred(onlinePlayer.uniqueId)
        }

        return InternalCommandBridge.async { InternalCommandBridge.requestPlayerUuid(playerName) }
    }
}

inline fun CommandAPICommand.offlinePlaynameToUuid(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {},
): CommandAPICommand =
    withArguments(OfflinePlaynameToUuid(nodeName).setOptional(optional).apply(block))