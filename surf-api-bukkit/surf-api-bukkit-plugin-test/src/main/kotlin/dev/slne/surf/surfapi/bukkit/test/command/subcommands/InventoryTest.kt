package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.open
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.TestInventoryView

class InventoryTest(name: String) : CommandAPICommand(name) {
    init {
        playerExecutor { player, _ ->
            TestInventoryView.open(player)
        }
    }
}