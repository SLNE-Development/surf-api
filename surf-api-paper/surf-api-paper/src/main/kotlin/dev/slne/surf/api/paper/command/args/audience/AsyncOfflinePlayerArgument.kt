package dev.slne.surf.api.paper.command.args.audience

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.StringArgument
import dev.slne.surf.api.paper.command.args.SuspendCustomArgument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

class AsyncOfflinePlayerArgument(nodeName: String) :
    SuspendCustomArgument<OfflinePlayer, String>(StringArgument(nodeName)) {

    override suspend fun CoroutineScope.parse(info: CustomArgumentInfo<String>) =
        withContext(Dispatchers.IO) {
            Bukkit.getOfflinePlayer(info.input)
        }

    init {
        this.replaceSuggestions(
            ArgumentSuggestions.stringCollection { viewerInfo ->
                Bukkit.getOnlinePlayers().filter { viewerInfo.sender !is Player || it.canSee(it) }
                    .map(Player::getName)
            }
        )
    }
}

inline fun CommandTree.asyncOfflinePlayerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandTree = then(
    AsyncOfflinePlayerArgument(nodeName).setOptional(optional).apply(block)
)

inline fun Argument<*>.asyncOfflinePlayerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): Argument<*> = then(
    AsyncOfflinePlayerArgument(nodeName).setOptional(optional).apply(block)
)

inline fun CommandAPICommand.asyncOfflinePlayerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandAPICommand =
    withArguments(AsyncOfflinePlayerArgument(nodeName).setOptional(optional).apply(block))