package dev.slne.surf.api.minestom.example.plugin

import dev.slne.surf.api.minestom.command.dsl.commandAPICommand
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import dev.slne.surf.api.minestom.extension.addListener
import dev.slne.surf.api.minestom.player.event.PlayerJoinEvent
import dev.slne.surf.api.minestom.plugin.PluginManager
import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.time.Duration.Companion.seconds

/**
 * A plugin that only uses the plugin api, `surf-api-minestom`.
 *
 * Its listeners, commands and coroutines are removed automatically when it is disabled.
 */
class ExamplePlugin : SurfMinestomPlugin() {
    private lateinit var greeting: String

    override suspend fun onLoad() {
        val file = dataDirectory.createDirectories().resolve("greeting.txt")
        if (!file.exists()) file.writeText("Welcome to the surf api example server!")
        greeting = file.readText().trim()

        logger.info("Loaded the greeting from {}", file)
    }

    override suspend fun onEnable() {
        eventNode.addListener<PlayerJoinEvent> { event ->
            event.player.sendMessage(Component.text(greeting, NamedTextColor.GREEN))
        }

        commandAPICommand("plugins") {
            playerExecutor { player, _ ->
                val plugins = PluginManager.instance.plugins.joinToString { "${it.meta.name} ${it.meta.version}" }
                player.sendMessage(Component.text("Plugins: $plugins"))
            }
        }

        scope.launch {
            while (true) {
                delay(300.seconds)
                logger.info("{} is still running", meta.name)
            }
        }
    }

    override suspend fun onDisable() {
        logger.info("Goodbye from {}", meta.name)
    }
}
