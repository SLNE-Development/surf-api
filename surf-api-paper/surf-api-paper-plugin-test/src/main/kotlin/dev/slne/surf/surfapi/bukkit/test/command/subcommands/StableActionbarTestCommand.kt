package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import com.github.shynixn.mccoroutine.folia.scope
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.slne.surf.api.core.actionbar.sendActionbar
import dev.slne.surf.api.core.util.dateTimeFormatter
import dev.slne.surf.surfapi.bukkit.test.plugin
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds

class StableActionbarTestCommand(name: String) : CommandAPICommand(name) {
    init {
        playerExecutor { sender, _ ->
            sender.sendActionbar(plugin.scope, 5.seconds) {
                appendInfoPrefix()
                info("Actionbar Test: ")
                variableValue(LocalDateTime.now().format(dateTimeFormatter))
            }
        }
    }
}