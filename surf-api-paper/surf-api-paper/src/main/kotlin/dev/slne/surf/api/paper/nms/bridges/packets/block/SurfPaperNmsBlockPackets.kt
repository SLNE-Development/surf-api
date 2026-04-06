@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.nms.bridges.packets.block

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import io.papermc.paper.math.BlockPosition
import org.bukkit.block.data.BlockData

@NmsUseWithCaution
interface SurfPaperNmsBlockPackets {
    fun updateBlockData(position: BlockPosition, blockData: BlockData): PacketOperation

    /**
     * Reset the block at the given position to its original state in the players world.
     *
     * @param position the position of the block to reset
     * @return the packet operation
     */
    fun resetBlock(position: BlockPosition): PacketOperation

    companion object : SurfPaperNmsBlockPackets by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsBlockPackets>()
