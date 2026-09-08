package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.paper.inventory.framework.open
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.testInventoryViewDsl
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.testPaginatedViewDsl

class InventoryTest(name: String) : CommandAPICommand(name) {
    init {
        playerExecutor { player, _ ->
//            TestInventoryView.open(player)
            testInventoryViewDsl.open(player)
        }

        subcommand("paginated") {
            playerExecutor { player, _ ->
                testPaginatedViewDsl.open(player)
            }
        }
    }
}