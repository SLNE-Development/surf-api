package dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.command.executors.anyExecutorSuspend
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler.event.TestAsyncEvent
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler.listener.TestAsyncEventListener
import org.bukkit.entity.Player
import java.util.*

class SurfAsyncEventHandlerTest(name: String) : CommandAPICommand(name) {
    init {
        subcommand("register") {
            anyExecutor { sender, _ ->
                TestAsyncEventListener.register()
                sender.sendText {
                    appendInfoPrefix()
                    info("Registered TestAsyncEvent listener")
                }
            }
        }

        subcommand("unregister") {
            anyExecutor { sender, _ ->
                TestAsyncEventListener.unregister()
                sender.sendText {
                    appendInfoPrefix()
                    info("Unregistered TestAsyncEvent listener")
                }
            }
        }

        subcommand("dispatch") {
            anyExecutorSuspend { sender, _ ->
                val playerUuid = if (sender is Player) {
                    sender.uniqueId
                } else {
                    UUID.randomUUID()
                }
                val event = TestAsyncEvent(playerUuid, "Test message from async event handler")
                event.call()
                sender.sendText {
                    appendInfoPrefix()
                    info("Dispatched TestAsyncEvent")
                }
            }
        }

        subcommand("status") {
            anyExecutor { sender, _ ->
                val lastEvent = TestAsyncEventListener.getLastEvent()
                if (lastEvent != null) {
                    sender.sendText {
                        appendInfoPrefix()
                        info("Last event: Player UUID=${lastEvent.playerUuid}, message='${lastEvent.message}'")
                    }
                    val handledTime = TestAsyncEventListener.getLastHandledTime()
                    sender.sendText {
                        appendInfoPrefix()
                        info("Handled at: $handledTime")
                    }
                } else {
                    sender.sendText {
                        appendWarningPrefix()
                        warning("No event received yet")
                    }
                }
            }
        }

        subcommand("clear") {
            anyExecutor { sender, _ ->
                TestAsyncEventListener.clearLastEvent()
                sender.sendText {
                    appendInfoPrefix()
                    info("Cleared last event")
                }
            }
        }
    }
}







