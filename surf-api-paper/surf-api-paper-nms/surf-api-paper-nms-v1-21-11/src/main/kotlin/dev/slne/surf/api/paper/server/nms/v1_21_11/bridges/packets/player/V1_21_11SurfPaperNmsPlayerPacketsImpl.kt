@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.player

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerPackets
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import io.papermc.paper.math.BlockPosition
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

@NmsUseWithCaution
class V1_21_11SurfPaperNmsPlayerPacketsImpl : SurfPaperNmsPlayerPackets {

    override fun openSignEditor(
        position: BlockPosition,
        frontSide: Boolean,
    ) = V1_21_11PacketOperationImpl.simple { ClientboundOpenSignEditorPacket(position.toNms(), frontSide) }

    override fun openInventory(
        syncId: Int,
        type: InventoryType,
        title: Component,
    ) = V1_21_11PacketOperationImpl.simple {
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
    ) = V1_21_11PacketOperationImpl.simple {
        ClientboundContainerSetSlotPacket(
            syncId,
            revision,
            slot,
            item.toNms()
        )
    }

    override fun closeInventory(syncId: Int) =
        V1_21_11PacketOperationImpl.simple { ClientboundContainerClosePacket(syncId) }
}
