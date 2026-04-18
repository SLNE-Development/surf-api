@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.player

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerPackets
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.V26_1PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
import io.papermc.paper.math.BlockPosition
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

@NmsUseWithCaution
@Suppress("ClassName")
class V26_1SurfPaperNmsPlayerPacketsImpl : SurfPaperNmsPlayerPackets {

    override fun openSignEditor(
        position: BlockPosition,
        frontSide: Boolean,
    ) = V26_1PacketOperationImpl.simple { ClientboundOpenSignEditorPacket(position.toNms(), frontSide) }

    override fun openInventory(
        syncId: Int,
        type: InventoryType,
        title: Component,
    ) = V26_1PacketOperationImpl.simple {
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
    ) = V26_1PacketOperationImpl.simple {
        ClientboundContainerSetSlotPacket(
            syncId,
            revision,
            slot,
            item.toNms()
        )
    }

    override fun closeInventory(syncId: Int) =
        V26_1PacketOperationImpl.simple { ClientboundContainerClosePacket(syncId) }
}
