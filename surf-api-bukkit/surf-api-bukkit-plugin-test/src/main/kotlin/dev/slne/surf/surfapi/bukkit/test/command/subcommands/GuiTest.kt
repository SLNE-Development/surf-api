package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.LiteralArgument
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import dev.slne.surf.surfapi.bukkit.test.gui.CounterGuiView
import dev.slne.surf.surfapi.bukkit.test.gui.PaginatedShopGuiView

/**
 * Command to test the new GUI framework.
 */
class GuiTest(name: String) : CommandAPICommand(name) {
    init {
        withPermission("surfapitest.use")
        
        // Counter subcommand
        withSubcommands(
            CommandAPICommand("counter")
                .executesPlayer(PlayerCommandExecutor { player, _ ->
                    CounterGuiView.open(player)
                }),
            
            // Shop subcommand
            CommandAPICommand("shop")
                .executesPlayer(PlayerCommandExecutor { player, _ ->
                    PaginatedShopGuiView.open(player)
                })
        )
    }
}
