package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.slne.surf.api.messages.actionbar.sendActionBar
import dev.slne.surf.api.paper.plugin.SurfPaperPluginRuntime
import dev.slne.surf.surfapi.bukkit.test.plugin
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

class StableActionbarTestCommand(name: String) : CommandAPICommand(name) {
    init {
        anyExecutor { sender, _ ->
            val scope = SurfPaperPluginRuntime.get().coroutineScope(plugin)
            sender.sendActionBar(scope, 5.seconds) {
                appendInfoPrefix()
                info("Aktuelle Sekunde: ")
                variableValue(LocalDateTime.now().second)
            }
        }
    }
}