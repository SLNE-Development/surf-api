package dev.slne.surf.surfapi.bukkit.test.command.subcommands.suspendexecution

import dev.jorel.commandapi.CommandAPICommand
import dev.slne.surf.surfapi.bukkit.api.command.executors.anyExecutorSuspend
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

class SuspendCommandExecutionDelayTest(name: String) : CommandAPICommand(name) {
    init {
        anyExecutorSuspend { sender, arguments ->
            sender.sendText {
                appendPrefix()
                info("Delay 3 seconds...")
            }
            delay(3.seconds)
            sender.sendText {
                appendPrefix()
                info("Done!")
            }
        }
    }
}