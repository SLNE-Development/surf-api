package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.surfapi.bukkit.test.config.ModernSerializerTestConfig

class ModernSerializerTestConfigCommand(name: String): CommandAPICommand(name) {

    init {
        subcommand("reload") {
            anyExecutor { _, _ ->
                ModernSerializerTestConfig.reloadFromFile()
            }
        }
    }
}