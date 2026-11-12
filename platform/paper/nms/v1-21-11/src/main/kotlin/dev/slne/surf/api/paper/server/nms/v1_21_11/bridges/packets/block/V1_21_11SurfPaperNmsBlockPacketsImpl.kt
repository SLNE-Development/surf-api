@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.block

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.block.SurfPaperNmsBlockPackets
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import io.papermc.paper.math.BlockPosition
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import org.bukkit.block.data.BlockData

@NmsUseWithCaution
class V1_21_11SurfPaperNmsBlockPacketsImpl : SurfPaperNmsBlockPackets {

    override fun updateBlockData(position: BlockPosition, blockData: BlockData) =
        V1_21_11PacketOperationImpl.simple {
            ClientboundBlockUpdatePacket(
                position.toNms(),
                blockData.toNms()
            )
        }


    override fun resetBlock(position: BlockPosition) = V1_21_11PacketOperationImpl.simple {
        ClientboundBlockUpdatePacket(
            it.toNms().level(),
            position.toNms()
        )
    }
}
