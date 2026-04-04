@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.nms.bridges.packets.player

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import io.papermc.paper.math.BlockPosition
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

@NmsUseWithCaution
interface SurfPaperNmsPlayerPackets {
    fun openSignEditor(position: BlockPosition, frontSide: Boolean): PacketOperation

    fun openInventory(syncId: Int, type: InventoryType, title: Component): PacketOperation

    fun setInventorySlot(syncId: Int, revision: Int, slot: Int, item: ItemStack): PacketOperation

    fun closeInventory(syncId: Int): PacketOperation

    companion object : SurfPaperNmsPlayerPackets by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsPlayerPackets>()
