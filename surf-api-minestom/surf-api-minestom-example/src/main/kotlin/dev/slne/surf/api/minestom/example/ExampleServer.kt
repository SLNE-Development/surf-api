package dev.slne.surf.api.minestom.example

import dev.slne.surf.api.minestom.chat.AsyncPlayerChatEvent
import dev.slne.surf.api.minestom.chat.ChatRenderer
import dev.slne.surf.api.minestom.chat.sendSignedMessage
import dev.slne.surf.api.minestom.server.chat.withSignedChat
import dev.slne.surf.api.minestom.server.configuration.ConfigurationTaskId
import dev.slne.surf.api.minestom.server.configuration.ConfigurationTasks
import dev.slne.surf.api.minestom.server.console.withConsole
import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.api.minestom.extension.*
import dev.slne.surf.api.minestom.server.npc.withNpcLib
import dev.slne.surf.api.minestom.server.plugins.withPlugins
import dev.slne.surf.api.minestom.permission.hasPermission
import dev.slne.surf.api.minestom.player.event.*
import dev.slne.surf.api.minestom.server.spark.withSpark
import dev.slne.surf.api.minestom.server.surfMinestomServer
import dev.slne.surf.api.minestom.server.visibility.withPlayerVisibility
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.InstanceContainer
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import kotlin.random.Random

private val LOGGER = ComponentLogger.logger("ExampleServer")

val SPAWN = Pos(0.5, 41.0, 0.5)

/**
 * A Minestom server that uses every feature of the surf api.
 *
 * Run it with `./gradlew :surf-api-minestom:surf-api-minestom-example:runServer` and join
 * `localhost:25565`.
 */
fun main() {
    val server = MinecraftServer.init(Auth.Online())
    val world = createWorld()

    surfMinestomServer {
        withConfigurationPhase {
            codeOfConduct { player ->
                "Hello ${player.username}! This is the surf api example server. Please be nice."
            }

            after(ConfigurationTasks.AWAIT_SETTINGS, ConfigurationTaskId("log_locale")) { context ->
                LOGGER.info(
                    "{} joins with locale {}",
                    context.player.username,
                    context.player.locale
                )
            }
        }

        withExamplePermissions()
        withSignedChat()
        withSpark()
        withNpcLib()
        withPlayerVisibility {
            maxVisible = 20
            activateAt = 25
            deactivateBelow = 15
        }
        withConsole()
        withPlugins()

        maxPlayers = 20
        listeners(ExampleListeners(world))
    }

    ExampleCommands.register()
    ExampleNpcs.spawn(world)

    val port = System.getProperty("example.port")?.toInt() ?: 25565
    server.start("0.0.0.0", port)
    LOGGER.info("Example server started on port {}", port)
}

private fun createWorld(): InstanceContainer = buildInstance {
    setChunkSupplier(::LightingChunk)
    generator {
        modify {
            fillHeight(0, 40, Block.GRASS_BLOCK)
        }
    }
}

private class ExampleListeners(private val world: InstanceContainer) : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener<AsyncPlayerConfigurationEvent> { event ->
            event.spawningInstance = world
            event.player.respawnPoint = SPAWN
            event.player.gameMode = GameMode.ADVENTURE
        }

        // Abilities are sent to the client right away, which is only possible once it plays
        node.addListener<PlayerSpawnEvent> { event ->
            if (event.isFirstSpawn) event.player.isAllowFlying = true
        }

        // Players with the bypass permission may join a full server
        node.addListener<AsyncPlayerCountEvent> { event ->
            if (event.result == AsyncPlayerCountEvent.Result.KICK_FULL &&
                event.player.hasPermission("example.bypass-full")
            ) {
                event.allow()
            }
        }

        // Runs on the player's configuration thread, so a stored location could be loaded here
        node.addListener<AsyncPlayerSpawnLocationEvent> { event ->
            event.position =
                SPAWN.add(Random.nextDouble(-3.0, 3.0), 0.0, Random.nextDouble(-3.0, 3.0))
        }

        node.addListener<PlayerJoinEvent> { event ->
            event.joinMessage = Component.text("+ ", NamedTextColor.GREEN).append(event.player.name)
        }

        node.addListener<PlayerQuitEvent> { event ->
            event.quitMessage = Component.text("- ", NamedTextColor.RED).append(event.player.name)
        }

        node.addListener<PlayerToggleFlightEvent> { event ->
            if (event.isFlying && event.player.isSneaking) {
                event.isCancelled = true
                event.player.sendMessage(
                    Component.text(
                        "No flying while sneaking!",
                        NamedTextColor.RED
                    )
                )
            }
        }

        // Runs off the tick thread, so the renderer may suspend or block
        AsyncPlayerChatEvent.addListener { event ->
            event.renderer = ChatRenderer.viewerUnaware { _, name, message ->
                Component.text()
                    .append(Component.text("[Example] ", NamedTextColor.AQUA))
                    .append(name)
                    .append(Component.text(": ", NamedTextColor.GRAY))
                    .append(message)
                    .build()
            }
        }
    }
}

/** Sends a message to everyone, keeping the signature [message] was sent with. */
internal fun broadcastSigned(
    message: SignedMessage,
    name: Component,
    rendered: Component,
) {
    for (player in ConnectionManager.onlinePlayers) {
        player.sendSignedMessage(message, name, rendered)
    }
}
