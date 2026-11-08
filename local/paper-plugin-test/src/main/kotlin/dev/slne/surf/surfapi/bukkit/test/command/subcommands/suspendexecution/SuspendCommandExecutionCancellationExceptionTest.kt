package dev.slne.surf.surfapi.bukkit.test.command.subcommands.suspendexecution

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class SuspendCommandExecutionCancellationExceptionTest(name: String) : CommandAPICommand(name) {
    init {
        anyExecutorSuspend { sender, arguments ->
            sender.sendText {
                info("Requesting data...")
            }
            delay(1.seconds)

            sender.sendText {
                info("Cancelling...")
            }

            cancel("Cancelled by user!")

            delay(2.seconds)
            sender.sendText {
                info("Done!")
            }
        }
    }
}