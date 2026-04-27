@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.nms.bridges.packets.player

import com.destroystokyo.paper.profile.PlayerProfile
import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import io.papermc.paper.math.BlockPosition
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import java.security.PublicKey
import java.time.Instant
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

    data class RemoteChatSessionData(
        val sessionId: UUID,
        val expiresAt: Instant,
        val key: PublicKey,
        val keySignature: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is RemoteChatSessionData) return false

            if (sessionId != other.sessionId) return false
            if (expiresAt != other.expiresAt) return false
            if (key != other.key) return false
            if (!keySignature.contentEquals(other.keySignature)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = sessionId.hashCode()
            result = 31 * result + expiresAt.hashCode()
            result = 31 * result + key.hashCode()
            result = 31 * result + keySignature.contentHashCode()
            return result
        }
    }

    companion object : SurfPaperNmsPlayerPackets by bridge {
        val INSTANCE get() = bridge

        init {
        }
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsPlayerPackets>()
