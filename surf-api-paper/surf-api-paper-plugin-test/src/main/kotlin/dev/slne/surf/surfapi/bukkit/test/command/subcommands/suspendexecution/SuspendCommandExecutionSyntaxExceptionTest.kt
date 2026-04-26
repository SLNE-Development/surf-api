package dev.slne.surf.surfapi.bukkit.test.command.subcommands.suspendexecution

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class SuspendCommandExecutionSyntaxExceptionTest(name: String) : CommandAPICommand(name) {
    init {
        anyExecutorSuspend { sender, arguments ->
            sender.sendText {
                info("Requesting data...")
            }
            delay(1.seconds)
            throw CommandAPI.failWithString("Some error!")
        }
    }
}