package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import org.bukkit.entity.Player

/**
 * Version-agnostic interface for glowing API lifecycle operations.
 *
 * Provides the operations needed by the GlowingListener event handler
 * without depending on version-specific implementations.
 */
interface GlowingLifecycleHandler {
    /**
     * Removes all glowing effects for a player (called on quit).
     */
    fun removeAllGlowingOnQuit(player: Player)

    /**
     * Gets block glow spawn operations for a chunk load event.
     *
     * @param player the player who loaded the chunk
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @param world the world of the chunk
     * @return a [PacketOperation] to spawn the glowing blocks, or null if none
     */
    fun getBlockGlowSpawnOperationForChunk(
        player: Player,
        chunkX: Int,
        chunkZ: Int,
        world: org.bukkit.World
    ): PacketOperation?
}
