package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.paper.packet.lore.SurfPaperPacketLoreHandler
import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

/**
 * Version-agnostic interface for managing packet lore handlers.
 *
 * Each version module provides an implementation that delegates to
 * its version-specific packet lore listener.
 */
interface PacketLoreRegistry {
    fun register(plugin: Plugin, identifier: NamespacedKey, listener: SurfPaperPacketLoreHandler, priority: Short)
    fun register(plugin: Plugin, listener: SurfPaperPacketLoreHandler, priority: Short)
    fun unregister(identifier: NamespacedKey)
    fun unregister(plugin: Plugin)
}
