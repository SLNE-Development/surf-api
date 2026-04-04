package dev.slne.surf.surfapi.bukkit.test.command.subcommands.suspendexecution

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.surfapi.bukkit.api.command.executors.anyExecutorSuspend
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class SuspendCommandExecutionUncheckedExceptionTest(name: String) : CommandAPICommand(name) {
    init {
        anyExecutorSuspend { sender, arguments ->
            sender.sendText {
                info("Requesting data...")
            }
            delay(1.seconds)
            error("Some error!")
        }
    }
}