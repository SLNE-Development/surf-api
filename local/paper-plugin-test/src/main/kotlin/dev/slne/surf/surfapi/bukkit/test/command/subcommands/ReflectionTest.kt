package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.reflection.SpigotRestartCommandProxy

class ReflectionTest(name: String) : CommandAPICommand(name) {
    init {
        anyExecutor { _, _ ->
            SpigotRestartCommandProxy.instance.restart()
        }
    }
}
