package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.slne.surf.api.command.SurfCommandUtil
import net.kyori.adventure.text.Component

class CommandExceptionTest(name: String) : CommandAPICommand(name) {
    init {
        anyExecutor { _, _ ->
            SurfCommandUtil.failWithMessage(Component.text("This is a test fail"))
        }
    }
}
