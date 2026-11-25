package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.slne.surf.api.messages.Colors
import net.kyori.adventure.text.Component

class PrefixConfigTest(name: String) : CommandAPICommand(name) {
    init {
        anyExecutor { sender, _ ->
            sender.sendMessage(Colors.PREFIX.append(Component.text("Prefix Config Test")))
        }
    }
}
