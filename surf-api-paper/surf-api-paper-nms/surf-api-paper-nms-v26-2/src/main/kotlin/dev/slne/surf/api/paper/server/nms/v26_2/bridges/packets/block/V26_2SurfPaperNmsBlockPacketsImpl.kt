@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.block

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.block.SurfPaperNmsBlockPackets
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.V26_1PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
import io.papermc.paper.math.BlockPosition
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import org.bukkit.block.data.BlockData

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2SurfPaperNmsBlockPacketsImpl : SurfPaperNmsBlockPackets {

    override fun updateBlockData(position: BlockPosition, blockData: BlockData) =
        V26_1PacketOperationImpl.simple {
            ClientboundBlockUpdatePacket(
                position.toNms(),
                blockData.toNms()
            )
        }


    override fun resetBlock(position: BlockPosition) = V26_1PacketOperationImpl.simple {
        ClientboundBlockUpdatePacket(
            it.toNms().level(),
            position.toNms()
        )
    }
}
