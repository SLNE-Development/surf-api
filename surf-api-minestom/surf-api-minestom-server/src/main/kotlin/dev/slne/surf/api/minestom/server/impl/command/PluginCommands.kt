package dev.slne.surf.api.minestom.server.impl.command

import dev.slne.surf.api.minestom.command.CommandAPI
import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.util.concurrent.ConcurrentHashMap

/**
 * Remembers which plugin registered which command, so that a plugin's commands go away with it.
 */
@InternalSurfApi
object PluginCommands {
    private val owned = ConcurrentHashMap<SurfMinestomPlugin, MutableSet<String>>()

    /** Assigns the command [name] to the plugin whose code is running, if any. */
    internal fun track(name: String) {
        val plugin = SurfMinestomPlugin.current() ?: return
        owned.computeIfAbsent(plugin) { ConcurrentHashMap.newKeySet() } += name
    }

    internal fun forget(name: String) {
        owned.values.forEach { names -> names -= name }
    }

    /** Unregisters every command [plugin] registered and returns how many there were. */
    fun unregisterAll(plugin: SurfMinestomPlugin): Int {
        val names = owned.remove(plugin) ?: return 0
        return names.count { name -> runCatching { CommandAPI.unregister(name) }.getOrDefault(false) }
    }
}
