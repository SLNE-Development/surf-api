package dev.slne.surf.surfapi.bukkit.api.packet

import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandlerSimple
import dev.slne.surf.surfapi.bukkit.api.util.getCallingPlugin
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

/**
 * The SurfBukkitPacketApi interface extends packet handling capabilities for Bukkit environments.
 *
 * Provides methods for registering and unregistering lore listeners, enabling dynamic modification
 * of item stack lore based on custom logic. Includes global registration for all items and utilities
 * for managing these listeners efficiently.
 *
 * Prefer the overloads that accept an explicit [Plugin] parameter over the deprecated ones,
 * as automatic caller-plugin detection via `getCallingPlugin` is unreliable across different
 * call-site configurations.
 */
interface SurfBukkitPacketApi {

    /**
     * Registers a listener for modifying the lore of a specific item stack identified by [identifier].
     *
     * @param identifier A unique key representing the item to listen for.
     * @param listener The listener that modifies the lore of the item stack.
     *
     * @deprecated Automatic plugin detection via `getCallingPlugin` is unreliable. Use
     * [registerPacketLoreListener(Plugin, NamespacedKey, SurfBukkitPacketLoreHandler)] instead
     * and pass your plugin instance explicitly.
     */
    @Deprecated(
        message = "Automatic plugin detection is unreliable. Pass your plugin instance explicitly.",
        replaceWith = ReplaceWith("registerPacketLoreListener(plugin, identifier, listener)")
    )
    fun registerPacketLoreListener(
        identifier: NamespacedKey,
        listener: SurfBukkitPacketLoreHandler
    ) {
        registerPacketLoreListener(getCallingPlugin(2), identifier, listener)
    }

    /**
     * Registers a listener for modifying the lore of a specific item stack identified by [identifier].
     *
     * This is the preferred overload. The [plugin] reference is used to properly manage the
     * listener lifecycle — all listeners registered under a plugin are automatically cleaned up
     * when [unregisterPacketLoreListener(Plugin)] is called.
     *
     * @param plugin The plugin registering the listener. Used for lifecycle management.
     * @param identifier A unique key representing the item to listen for.
     * @param listener The listener that modifies the lore of the item stack.
     */
    fun registerPacketLoreListener(
        plugin: Plugin,
        identifier: NamespacedKey,
        listener: SurfBukkitPacketLoreHandler
    )

    /**
     * Registers a simplified listener for modifying the lore of a specific item stack identified by [identifier].
     *
     * Delegates to [registerPacketLoreListener(Plugin, NamespacedKey, SurfBukkitPacketLoreHandler)].
     *
     * @param identifier A unique key representing the item to listen for.
     * @param listener The simplified lore listener that focuses solely on modifying the lore list.
     *
     * @deprecated Automatic plugin detection is unreliable. Use
     * [registerPacketLoreListener(Plugin, NamespacedKey, SurfBukkitPacketLoreHandlerSimple)] instead
     * and pass your plugin instance explicitly.
     */
    @Deprecated(
        message = "Automatic plugin detection is unreliable. Pass your plugin instance explicitly.",
        replaceWith = ReplaceWith("registerPacketLoreListener(plugin, identifier, listener)")
    )
    fun registerPacketLoreListener(
        identifier: NamespacedKey,
        listener: SurfBukkitPacketLoreHandlerSimple
    ) {
        registerPacketLoreListener(getCallingPlugin(2), identifier, listener)
    }

    /**
     * Registers a simplified listener for modifying the lore of a specific item stack identified by [identifier].
     *
     * This is the preferred overload. Delegates to
     * [registerPacketLoreListener(Plugin, NamespacedKey, SurfBukkitPacketLoreHandler)].
     * The [plugin] reference is used to properly manage the listener lifecycle.
     *
     * @param plugin The plugin registering the listener. Used for lifecycle management.
     * @param identifier A unique key representing the item to listen for.
     * @param listener The simplified lore listener that focuses solely on modifying the lore list.
     */
    fun registerPacketLoreListener(
        plugin: Plugin,
        identifier: NamespacedKey,
        listener: SurfBukkitPacketLoreHandlerSimple
    ) {
        registerPacketLoreListener(plugin, identifier, listener as SurfBukkitPacketLoreHandler)
    }

    /**
     * Registers a lore listener globally to handle lore modifications for all items.
     *
     * Unlike the key-based overloads, this listener fires for every item stack regardless of
     * its identifier. Use this only when you genuinely need to intercept all items, as it has
     * a broader performance impact.
     *
     * @param plugin The plugin registering the listener. Used for lifecycle management.
     * @param listener The lore listener to handle lore modifications globally.
     */
    fun registerPacketLoreListenerGlobal(
        plugin: Plugin,
        listener: SurfBukkitPacketLoreHandler
    )

    /**
     * Registers a simplified lore listener globally for all items.
     *
     * Delegates to [registerPacketLoreListenerGlobal(Plugin, SurfBukkitPacketLoreHandler)].
     *
     * @param plugin The plugin registering the listener. Used for lifecycle management.
     * @param listener The simplified lore listener that focuses solely on the lore list.
     */
    fun registerPacketLoreListenerGlobal(
        plugin: Plugin,
        listener: SurfBukkitPacketLoreHandlerSimple
    ) {
        registerPacketLoreListenerGlobal(plugin, listener as SurfBukkitPacketLoreHandler)
    }

    /**
     * Unregisters the packet lore listener associated with the given [identifier].
     *
     * @param identifier The key identifying the listener to unregister.
     */
    fun unregisterPacketLoreListener(identifier: NamespacedKey)

    /**
     * Unregisters all packet lore listeners associated with the given [plugin].
     *
     * Call this during plugin shutdown to ensure all listeners registered under this plugin
     * are properly cleaned up.
     *
     * @param plugin The plugin whose listeners should be unregistered.
     */
    fun unregisterPacketLoreListener(plugin: Plugin)

    companion object {
        @JvmStatic
        val INSTANCE get() = api
    }
}

private val api = requiredService<SurfBukkitPacketApi>()