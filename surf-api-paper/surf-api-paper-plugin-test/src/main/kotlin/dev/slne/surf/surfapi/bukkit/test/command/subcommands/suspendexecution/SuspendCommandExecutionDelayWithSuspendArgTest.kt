package dev.slne.surf.surfapi.bukkit.test.command.subcommands.suspendexecution

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.StringArgument
import dev.jorel.commandapi.kotlindsl.arguments
import dev.slne.surf.surfapi.bukkit.api.command.args.SuspendCustomArgument
import dev.slne.surf.surfapi.bukkit.api.command.executors.anyExecutorSuspend
import dev.slne.surf.surfapi.core.api.command.args.awaiting
import dev.slne.surf.surfapi.core.api.messages.adventure.sendText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import kotlin.time.Duration.Companion.seconds

class SuspendCommandExecutionDelayWithSuspendArgTest(name: String) : CommandAPICommand(name) {
    init {
        arguments(SuspendPlayerArgument("player"))

        anyExecutorSuspend { sender, arguments ->
            val player = arguments.awaiting<Player>("player")

            sender.sendText {
                appendInfoPrefix()
                info("Delay 3 seconds...")
            }
            delay(3.seconds)
            sender.sendText {
                appendInfoPrefix()
                info("Done!")
                appendSpace()
                info("Player: ")
                append(player.displayName())
            }
        }
    }

    class SuspendPlayerArgument(nodeName: String) :
        SuspendCustomArgument<Player, String>(StringArgument(nodeName)) {
        override suspend fun CoroutineScope.parse(info: CustomArgumentInfo<String>): Player {
            info.sender.sendText {
                appendInfoPrefix()
                info("Delaying in command parsing")
            }
            delay(2.seconds)
            val playerName = info.input()

            info.sender.sendText {
                appendInfoPrefix()
                info("Done delaying in command parsing")
            }

            val player =
                Bukkit.getPlayer(playerName) ?: throw CustomArgumentException.fromMessageBuilder(
                    MessageBuilder()
                        .append("Player ")
                        .appendArgInput()
                        .append(" not found!")
                )

            return player
        }
    }

}