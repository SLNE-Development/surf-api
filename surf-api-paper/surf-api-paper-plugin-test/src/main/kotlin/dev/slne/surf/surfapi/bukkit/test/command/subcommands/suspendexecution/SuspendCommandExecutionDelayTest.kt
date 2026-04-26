package dev.slne.surf.surfapi.bukkit.test.command.subcommands.suspendexecution

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class SuspendCommandExecutionDelayTest(name: String) : CommandAPICommand(name) {
    init {
        anyExecutorSuspend { sender, arguments ->
            sender.sendText {
                appendInfoPrefix()
                info("Delay 3 seconds...")
            }
            delay(3.seconds)
            sender.sendText {
                appendInfoPrefix()
                info("Done!")
            }
        }
    }
}