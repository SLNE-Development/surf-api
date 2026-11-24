package dev.slne.minestom.example.plugin

import dev.slne.surf.api.command.CommandRegistrar
import dev.slne.surf.api.minestom.command.dsl.commandAPICommand
import dev.slne.surf.api.minestom.command.dsl.playerExecutor
import dev.slne.surf.api.minestom.plugin.PluginManager
import net.kyori.adventure.text.Component
import org.springframework.stereotype.Component as SpringComponent

/**
 * Registers the plugin's commands when the plugin is enabled; the commands are unregistered when it
 * is disabled.
 */
@SpringComponent
class ExampleCommands : CommandRegistrar {

    override fun register() {
        commandAPICommand("plugins") {
            playerExecutor { player, _ ->
                val plugins = PluginManager.instance.plugins.joinToString {
                    "${it.meta.name} ${it.meta.version}"
                }
                player.sendMessage(Component.text("Plugins: $plugins"))
            }
        }
    }
}
