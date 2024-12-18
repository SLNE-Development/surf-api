package dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.player

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player.SurfBukkitNmsPlayerPackets
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.PacketOperationImpl
import dev.slne.surf.surfapi.bukkit.server.nms.toNms
import dev.slne.surf.surfapi.core.api.util.checkInstantiationByServiceLoader
import io.papermc.paper.math.BlockPosition
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

@AutoService(SurfBukkitNmsPlayerPackets::class)
@NmsUseWithCaution
class SurfBukkitNmsPlayerPacketsImpl : SurfBukkitNmsPlayerPackets {
    init {
        checkInstantiationByServiceLoader()
    }

    override fun openSignEditor(
        position: BlockPosition,
        frontSide: Boolean,
    ) = PacketOperationImpl.simple { ClientboundOpenSignEditorPacket(position.toNms(), frontSide) }

    override fun openInventory(
        syncId: Int,
        type: InventoryType,
        title: Component,
    ) = PacketOperationImpl.simple {
        ClientboundOpenScreenPacket(
            syncId,
            type.toNms(),
            title.toNms()
        )
    }

    override fun setInventorySlot(
        syncId: Int,
        revision: Int,
        slot: Int,
        item: ItemStack,
    ) = PacketOperationImpl.simple {
        ClientboundContainerSetSlotPacket(
            syncId,
            revision,
            slot,
            item.toNms()
        )
    }

    override fun closeInventory(syncId: Int) =
        PacketOperationImpl.simple { ClientboundContainerClosePacket(syncId) }
}
