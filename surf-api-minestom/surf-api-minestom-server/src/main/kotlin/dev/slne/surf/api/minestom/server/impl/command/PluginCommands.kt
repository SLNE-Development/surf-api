package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks the commands registered by each plugin so they can be unregistered when the plugin unloads.
 *
 * The map is concurrent; entries are added by the platform on registration and removed on
 * unregistration.
 */
@InternalSurfApi
object PluginCommands {
    private val owned = ConcurrentHashMap<SurfMinestomPlugin, MutableSet<String>>()

    /**
     * Assigns the command [name] to the plugin whose code is currently running, if any.
     */
    internal fun track(name: String) {
        val plugin = SurfMinestomPlugin.current() ?: return
        owned.computeIfAbsent(plugin) { ConcurrentHashMap.newKeySet() } += name
    }

    /**
     * Removes the command [name] from every plugin's set.
     */
    internal fun forget(name: String) {
        owned.values.forEach { names -> names -= name }
    }

    /**
     * Unregisters every command registered by [plugin] and returns how many were unregistered.
     *
     * A command that fails to unregister is skipped and not counted.
     */
    fun unregisterAll(plugin: SurfMinestomPlugin): Int {
        val names = owned.remove(plugin) ?: return 0
        return names.count { name -> runCatching { CommandAPI.unregister(name) }.getOrDefault(false) }
    }
}
