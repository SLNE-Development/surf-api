package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import com.github.shynixn.mccoroutine.folia.launch
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.literalArgument
import dev.slne.surf.surfapi.bukkit.server.plugin
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.openClassBasedExampleGui
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.inventory.openDslExampleGui
import org.bukkit.entity.Player

class NewInventoryTest(name: String) : CommandAPICommand(name) {
    init {
        literalArgument("class") {
            anyExecutor { sender, _ ->
                if (sender is Player) {
                    plugin.launch(sender) {
                        sender.openClassBasedExampleGui()
                    }
                }
            }
        }

        literalArgument("dsl") {
            anyExecutor { sender, _ ->
                if (sender is Player) {
                    plugin.launch(sender) {
                        sender.openDslExampleGui()
                    }
                }
            }
        }
    }
}
