package dev.slne.surf.api.paper.command.args.audience

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.StringArgument
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

class OfflinePlayerArgument(nodeName: String) :
    CustomArgument<OfflinePlayer, String>(StringArgument(nodeName), { info ->
        Bukkit.getOfflinePlayer(info.input)
    }) {
    init {
        this.replaceSuggestions(
            ArgumentSuggestions.stringCollection { viewerInfo ->
                Bukkit.getOnlinePlayers().filter { viewerInfo.sender !is Player || it.canSee(it) }
                    .map(Player::getName)
            }
        )
    }
}

inline fun CommandTree.offlinePlayerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandTree = then(
    OfflinePlayerArgument(nodeName).setOptional(optional).apply(block)
)

inline fun Argument<*>.offlinePlayerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): Argument<*> = then(
    OfflinePlayerArgument(nodeName).setOptional(optional).apply(block)
)

inline fun CommandAPICommand.offlinePlayerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandAPICommand =
    withArguments(OfflinePlayerArgument(nodeName).setOptional(optional).apply(block))