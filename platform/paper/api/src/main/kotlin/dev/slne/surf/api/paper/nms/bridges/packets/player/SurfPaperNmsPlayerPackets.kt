@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.nms.bridges.packets.player

import com.destroystokyo.paper.profile.PlayerProfile
import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.data.chat.RemoteChatSessionData
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import io.papermc.paper.math.BlockPosition
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import java.util.*

@NmsUseWithCaution
interface SurfPaperNmsPlayerPackets {
    fun openSignEditor(position: BlockPosition, frontSide: Boolean): PacketOperation

    fun openInventory(syncId: Int, type: InventoryType, title: Component): PacketOperation

    fun setInventorySlot(syncId: Int, revision: Int, slot: Int, item: ItemStack): PacketOperation

    fun closeInventory(syncId: Int): PacketOperation

    fun createNewPlayerInfoUpdate(
        profileId: UUID,
        profile: PlayerProfile,
        listed: Boolean,
        latency: Int,
        gameMode: GameMode,
        displayName: Component?,
        showHat: Boolean,
        listOrder: Int,
        chatSession: RemoteChatSessionData?
    ): PacketOperation

    fun removePlayerInfoUpdate(profileIds: List<UUID>): PacketOperation

    companion object : SurfPaperNmsPlayerPackets by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsPlayerPackets>()
