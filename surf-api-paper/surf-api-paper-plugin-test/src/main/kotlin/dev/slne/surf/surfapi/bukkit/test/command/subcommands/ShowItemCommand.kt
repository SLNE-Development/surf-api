package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.messages.adventure.sendText

class ShowItemCommand(name: String) : CommandAPICommand(name) {
    init {
        playerExecutor { player, arguments ->
            val item = player.inventory.itemInMainHand
            player.sendText {
                info("Item in hand: ")
                append {
                    append(item.displayName())
                    hoverEvent(item)
                }
            }
        }
    }
}