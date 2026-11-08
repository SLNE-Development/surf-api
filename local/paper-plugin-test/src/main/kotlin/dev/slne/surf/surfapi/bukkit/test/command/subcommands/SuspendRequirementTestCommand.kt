package dev.slne.surf.surfapi.bukkit.test.command.subcommands

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.*
import dev.slne.surf.api.paper.command.requirement.withSuspendPlayerRequirement
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class SuspendRequirementTestCommand(name: String) : CommandAPICommand(name) {
    private val shown = ConcurrentHashMap.newKeySet<UUID>()

    init {
        subcommand("updateCommands") {
            playerExecutor { player, _ ->
                player.updateCommands()
            }
        }

        subcommand("showCommand") {
            booleanArgument("show")

            playerExecutor { player, arguments ->
                val show: Boolean by arguments
                if (show) {
                    shown.add(player.uniqueId)
                } else {
                    shown.remove(player.uniqueId)
                }
            }
        }

        subcommand("conditionalCommand") {
            withSuspendPlayerRequirement { player ->
                delay(10.seconds)
                player.uniqueId in shown
            }

            anyExecutor { sender, _ ->
                sender.sendMessage("This command is only available to players who have shown it")
            }
        }
    }
}