package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.surfapi.bukkit.test.gui.TestGuiView

class NewGuiTest(name: String) : CommandAPICommand(name) {
    init {
        playerExecutor { player, _ ->
            TestGuiView.open(player)
        }
    }
}
