package dev.slne.surf.api.paper.packet

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandler
import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandlerSimple
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
interface SurfPaperPacketApi {

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
        listener: SurfPaperPacketLoreHandler
    )

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
        listener: SurfPaperPacketLoreHandlerSimple
    ) {
        registerPacketLoreListener(plugin, identifier, listener as SurfPaperPacketLoreHandler)
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
        listener: SurfPaperPacketLoreHandler
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
        listener: SurfPaperPacketLoreHandlerSimple
    ) {
        registerPacketLoreListenerGlobal(plugin, listener as SurfPaperPacketLoreHandler)
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

    companion object : SurfPaperPacketApi by api {
        val INSTANCE get() = api
    }
}

private val api = requiredService<SurfPaperPacketApi>()