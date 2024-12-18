package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.block

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.block.SurfBukkitNmsBlockPackets
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.PacketOperationImpl
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import io.papermc.paper.math.BlockPosition
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import org.bukkit.block.data.BlockData

@AutoService(SurfBukkitNmsBlockPackets::class)
@NmsUseWithCaution
class SurfBukkitNmsBlockPacketsImpl : SurfBukkitNmsBlockPackets {
    init {
        checkInstantiationByServiceLoader()
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

