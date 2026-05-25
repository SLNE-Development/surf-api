package dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.kotlindsl.anyExecutor
import dev.jorel.commandapi.kotlindsl.playerExecutor
import dev.jorel.commandapi.kotlindsl.subcommand
import dev.slne.surf.api.core.messages.adventure.sendText
import dev.slne.surf.api.paper.event.playtime.AfkStateChangeEvent
import dev.slne.surf.surfapi.bukkit.test.command.subcommands.eventhandler.listener.PlayerAfkStateChangeEventListener

class SurfSyncEventHandlerTest(name: String) : CommandAPICommand(name) {
    init {

        subcommand("register") {
            anyExecutor { sender, _ ->
                PlayerAfkStateChangeEventListener.register()
                sender.sendText {
                    appendInfoPrefix()
                    info("Registered PlayerAfkStateChangeEvent listener")
                }
            }
        }

        subcommand("unregister") {
            anyExecutor { sender, _ ->
                PlayerAfkStateChangeEventListener.unregister()
                sender.sendText {
                    appendInfoPrefix()
                    info("Unregistered PlayerAfkStateChangeEvent listener")
                }
            }
        }

        subcommand("dispatch") {
            playerExecutor { sender, _ ->
                val playerUuid = sender.uniqueId
                val event = AfkStateChangeEvent(playerUuid, fromState = false, toState = true)
                event.call()
                sender.sendText {
                    appendInfoPrefix()
                    info("Dispatched PlayerAfkStateChangeEvent for ${sender.name}")
                }
            }
        }

        subcommand("status") {
            anyExecutor { sender, _ ->
                val lastEvent = PlayerAfkStateChangeEventListener.getLastEvent()
                if (lastEvent != null) {
                    sender.sendText {
                        appendInfoPrefix()
                        info("Last event: Player UUID=${lastEvent.playerUuid}, fromState=${lastEvent.fromState}, toState=${lastEvent.toState}")
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
                PlayerAfkStateChangeEventListener.clearLastEvent()
                sender.sendText {
                    appendInfoPrefix()
                    info("Cleared last event")
                }
            }
        }
    }
}