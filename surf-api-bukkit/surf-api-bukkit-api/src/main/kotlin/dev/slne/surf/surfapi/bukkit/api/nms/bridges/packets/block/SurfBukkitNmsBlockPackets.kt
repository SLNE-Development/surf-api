package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.block

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.core.api.util.requiredService
import io.papermc.paper.math.BlockPosition
import org.bukkit.block.data.BlockData

@NmsUseWithCaution
interface SurfBukkitNmsBlockPackets {
    fun updateBlockData(position: BlockPosition, blockData: BlockData): PacketOperation

    /**
     * Reset the block at the given position to its original state in the players world.
     *
     * @param position the position of the block to reset
     * @return the packet operation
     */
    fun resetBlock(position: BlockPosition): PacketOperation

    companion object {
        val instance = requiredService<SurfBukkitNmsBlockPackets>()
    }
}

@NmsUseWithCaution
val nmsBlockPackets get() = SurfBukkitNmsBlockPackets.instance
