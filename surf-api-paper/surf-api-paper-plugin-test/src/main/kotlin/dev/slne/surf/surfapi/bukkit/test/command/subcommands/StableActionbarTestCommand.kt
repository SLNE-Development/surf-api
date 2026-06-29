package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import com.github.shynixn.mccoroutine.folia.scope
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.slne.surf.api.core.actionbar.sendActionBar
import dev.slne.surf.surfapi.bukkit.test.plugin
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

class StableActionbarTestCommand(name: String) : CommandAPICommand(name) {
    init {
        anyExecutor { sender, _ ->
            sender.sendActionBar(plugin.scope, 5.seconds) {
                appendInfoPrefix()
                info("Aktuelle Sekunde: ")
                variableValue(LocalDateTime.now().second)
            }
        }
    }
}