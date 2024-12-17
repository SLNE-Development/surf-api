package dev.slne.surf.surfapi.bukkit.api.packet

import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandler
import dev.slne.surf.surfapi.bukkit.api.packet.lore.SurfBukkitPacketLoreHandlerSimple
import dev.slne.surf.surfapi.core.api.util.requiredService
import net.kyori.adventure.key.Key
import org.bukkit.plugin.Plugin

/**
 * The SurfBukkitPacketApi interface extends packet handling capabilities for Bukkit environments.
 *
 * It provides methods for registering and unregistering lore listeners, enabling dynamic modification
 * of item stack lore based on custom logic. It also includes global registration methods for listening
 * to all items and utilities for managing these listeners efficiently.
 *
 * This API allows developers to enhance item lore dynamically, providing a flexible system for plugins
 * that need to alter or interact with lore without directly modifying the underlying item stack data.
 */
interface SurfBukkitPacketApi {

    /**
     * Registers a listener for modifying the lore of a specific item stack identified by the given key.
     *
     * @param identifier A unique key representing the item to listen for. Must not be null.
     * @param listener   The listener that modifies the lore of the item stack. Must not be null.
     *
     * Example Usage:
     * ```
     * val key = Key.key("myplugin", "custom_item")
     * surfBukkitPacketApi.registerPacketLoreListener(key, SurfBukkitPacketLoreHandler { lore, _, _ ->
     *     lore.add(Component.text("Special Lore!"))
     * })
     * ```
     */
    fun registerPacketLoreListener(
        identifier: Key,
        listener: SurfBukkitPacketLoreHandler
    )

    /**
     * Registers a simplified packet lore listener for a specific item.
     *
     * @param identifier A unique key representing the item to listen for. Must not be null.
     * @param listener   The simplified packet lore listener that focuses solely on modifying the lore list.
     *
     * This method delegates to the standard [registerPacketLoreListener] implementation.
     */
    fun registerPacketLoreListener(
        identifier: Key,
        listener: SurfBukkitPacketLoreHandlerSimple
    ) {
        registerPacketLoreListener(identifier, listener as SurfBukkitPacketLoreHandler)
    }

    /**
     * Registers a packet lore listener globally to handle lore modifications for all items.
     *
     * @param plugin   The plugin registering the listener. Used to manage lifecycle and cleanup.
     * @param listener The lore listener to handle lore modifications globally.
     *
     * Example Usage:
     * ```
     * surfBukkitPacketApi.registerPacketLoreListenerGlobal(myPlugin, SurfBukkitPacketLoreHandler { lore, _, _ ->
     *     lore.add(Component.text("Global Lore Modification"))
     * })
     * ```
     */
    fun registerPacketLoreListenerGlobal(
        plugin: Plugin,
        listener: SurfBukkitPacketLoreHandler
    )

    /**
     * Registers a simplified packet lore listener globally for all items.
     *
     * @param plugin   The plugin registering the listener. Used for proper cleanup during plugin shutdown.
     * @param listener The simplified lore listener that focuses solely on the lore list.
     *
     * This method delegates to the standard [registerPacketLoreListenerGlobal] implementation.
     */
    fun registerPacketLoreListenerGlobal(
        plugin: Plugin,
        listener: SurfBukkitPacketLoreHandlerSimple
    ) {
        registerPacketLoreListenerGlobal(plugin, listener as SurfBukkitPacketLoreHandler)
    }

    /**
     * Unregisters a previously registered packet lore listener identified by the given key.
     *
     * @param identifier The key identifying the listener to unregister.
     *
     * Example Usage:
     * ```
     * surfBukkitPacketApi.unregisterPacketLoreListener(Key.key("myplugin", "custom_item"))
     * ```
     */
    fun unregisterPacketLoreListener(identifier: Key)

    /**
     * Unregisters all packet lore listeners associated with the given plugin.
     *
     * @param plugin The plugin whose listeners should be unregistered.
     *
     * Example Usage:
     * ```
     * surfBukkitPacketApi.unregisterPacketLoreListener(myPlugin)
     * ```
     */
    fun unregisterPacketLoreListener(plugin: Plugin)

    companion object {
        val instance = requiredService<SurfBukkitPacketApi>()
    }
}

val surfBukkitPacketApi get() = SurfBukkitPacketApi.instance
