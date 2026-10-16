package dev.slne.surf.api.minestom.example

import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.command.dsl.commandAPICommand
import dev.slne.surf.api.minestom.command.dsl.integerArgument
import dev.slne.surf.api.minestom.command.dsl.playerArgument
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import dev.slne.surf.api.minestom.command.dsl.playerExecutorSuspend
import dev.slne.surf.api.minestom.command.dsl.signedMessageArgument
import dev.slne.surf.api.minestom.command.dsl.subcommand
import dev.slne.surf.api.minestom.coroutine.withEntity
import dev.slne.surf.api.minestom.highlight.BlockHighlights
import dev.slne.surf.api.minestom.visibility.PlayerVisibility
import kotlinx.coroutines.delay
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import kotlin.time.Duration.Companion.seconds

/**
 * Commands built with the CommandAPI port of the surf api.
 */
internal object ExampleCommands {

    fun register() {
        commandAPICommand("example") {
            withSubcommand(subcommand("highlight") {
                integerArgument("seconds", min = 1, max = 60, optional = true)
                playerExecutorSuspend { player, args ->
                    val seconds = args.getOptional<Int>("seconds") ?: 5
                    val position = player.position.sub(0.0, 1.0, 0.0)
                    val instance = player.instance ?: return@playerExecutorSuspend

                    BlockHighlights.show(player, instance, position, NamedTextColor.GOLD)
                    delay(seconds.seconds)
                    player.withEntity { BlockHighlights.hide(it, instance, position) }
                }
            })

            withSubcommand(subcommand("hide") {
                playerArgument("target")
                playerExecutor { player, args ->
                    val target = args.get<Player>("target")
                    PlayerVisibility.updateViewerRule(player) { entity -> entity !== target }
                    player.sendMessage(Component.text("${target.username} is now hidden from you"))
                }
            })

            withSubcommand(subcommand("showall") {
                playerExecutor { player, _ ->
                    PlayerVisibility.updateViewerRule(player, null)
                    player.sendMessage(Component.text("Everyone is visible again"))
                }
            })
        }

        commandAPICommand("gamemode") {
            withAliases("gm")
            withPermission("example.gamemode")
            for (mode in GameMode.entries) {
                withSubcommand(subcommand(mode.name.lowercase()) {
                    playerExecutor { player, _ ->
                        player.gameMode = mode
                        player.sendMessage(Component.text("Game mode set to ${mode.name.lowercase()}"))
                    }
                })
            }
        }

        commandAPICommand("stop") {
            withPermission("example.stop")
            anyExecutor { _, _ -> MinecraftServer.stopCleanly() }
        }

        // The message keeps the signature the client produced for the argument
        commandAPICommand("shout") {
            signedMessageArgument("message")
            playerExecutor { player, args ->
                val message = args.get<SignedMessage>("message")
                val name = Component.text(player.username, NamedTextColor.YELLOW)
                val rendered = Component.text()
                    .append(name)
                    .append(Component.text(" shouts: ", NamedTextColor.GOLD))
                    .append(Component.text(message.message()))
                    .build()

                broadcastSigned(message, name, rendered)
            }
        }
    }
}
