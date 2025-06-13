package dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack

@NmsUseWithCaution
interface ContainerClickPacket : NmsServerboundPacket {
    val view: InventoryView
    val containerId: Int
    val stateId: Int
    val slotNumber: Int
    val buttonNumber: Int
    val clickType: WindowClickType
    val changedSlots: Int2ObjectMap<ItemStack>
    val carriedItem: ItemStack
    val whoClicked: Player

    enum class WindowClickType {
        PICKUP, QUICK_MOVE, SWAP, CLONE, THROW, QUICK_CRAFT, PICKUP_ALL, UNKNOWN;
    }
}