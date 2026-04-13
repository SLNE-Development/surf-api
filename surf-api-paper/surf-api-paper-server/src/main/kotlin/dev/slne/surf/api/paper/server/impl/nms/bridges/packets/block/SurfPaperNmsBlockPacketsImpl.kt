@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.server.impl.nms.bridges.packets.block

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.block.SurfPaperNmsBlockPackets
import dev.slne.surf.api.paper.server.impl.nms.bridges.packets.PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.toNms
import io.papermc.paper.math.BlockPosition
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import org.bukkit.block.data.BlockData

@NmsUseWithCaution
class SurfPaperNmsBlockPacketsImpl : SurfPaperNmsBlockPackets {
    init {
    }

    override fun updateBlockData(position: BlockPosition, blockData: BlockData) =
        PacketOperationImpl.simple {
            ClientboundBlockUpdatePacket(
                position.toNms(),
                blockData.toNms()
            )
        }


    override fun resetBlock(position: BlockPosition) = PacketOperationImpl.simple {
        ClientboundBlockUpdatePacket(
            it.toNms().level(),
            position.toNms()
        )
    }
}

