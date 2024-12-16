package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.core.api.util.requiredService
import io.papermc.paper.math.BlockPosition
import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack

@NmsUseWithCaution
interface SurfBukkitNmsPlayerPackets {
    fun openSignEditor(position: BlockPosition, frontSide: Boolean): PacketOperation

    fun openInventory(syncId: Int, type: InventoryType, title: Component): PacketOperation

    fun setInventorySlot(syncId: Int, revision: Int, slot: Int, item: ItemStack): PacketOperation

    fun closeInventory(syncId: Int): PacketOperation

    companion object {
        val instance = requiredService<SurfBukkitNmsPlayerPackets>()
    }
}

@NmsUseWithCaution
val nmsPlayerPacketsBridge get() = SurfBukkitNmsPlayerPackets.instance
