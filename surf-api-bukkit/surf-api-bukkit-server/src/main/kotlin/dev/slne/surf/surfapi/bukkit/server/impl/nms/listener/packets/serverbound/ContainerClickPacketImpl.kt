package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.ContainerClickPacket
import dev.slne.surf.surfapi.bukkit.server.nms.toWindowClickType
import dev.slne.surf.surfapi.core.api.util.mutableInt2ObjectMapOf
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import org.bukkit.entity.Player
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack

@NmsUseWithCaution
class ContainerClickPacketImpl(
    nmsPacket: ServerboundContainerClickPacket,
    override val view: InventoryView,
    override val whoClicked: Player,
) : NmsServerboundPacketImpl<ServerboundContainerClickPacket>(nmsPacket), ContainerClickPacket {
    override val containerId get() = nmsPacket.containerId
    override val stateId get() = nmsPacket.stateId
    override val slotNumber get() = nmsPacket.slotNum
    override val buttonNumber get() = nmsPacket.buttonNum
    override val clickType get() = nmsPacket.clickType.toWindowClickType()
    override val changedSlots: Int2ObjectMap<ItemStack>
        get() = run {
            val map = mutableInt2ObjectMapOf<ItemStack>()

            nmsPacket.changedSlots.forEach { (key, value) ->
                map[key] = value.bukkitStack
            }

            map
        }
    override val carriedItem get() = nmsPacket.carriedItem.bukkitStack
}