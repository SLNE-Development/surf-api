package dev.slne.surf.api.paper.command.args.audience

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandTree
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.CustomArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.slne.surf.api.core.messages.adventure.buildText
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class PlayerArgument(nodeName: String) :
    CustomArgument<Player, String>(StringArgument(nodeName), { info ->
        Bukkit.getPlayer(info.input)?.takeIf {
            info.sender !is Player || it.canSee(it)
        } ?: throw CustomArgumentException.fromAdventureComponent(
            buildText {
                appendErrorPrefix()
                error("Der Spieler wurde nicht gefunden.")
            })
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

inline fun CommandTree.playerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandTree = then(
    PlayerArgument(nodeName).setOptional(optional).apply(block)
)

inline fun Argument<*>.playerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): Argument<*> = then(
    PlayerArgument(nodeName).setOptional(optional).apply(block)
)

inline fun CommandAPICommand.playerArgument(
    nodeName: String,
    optional: Boolean = false,
    block: Argument<*>.() -> Unit = {}
): CommandAPICommand =
    withArguments(PlayerArgument(nodeName).setOptional(optional).apply(block))