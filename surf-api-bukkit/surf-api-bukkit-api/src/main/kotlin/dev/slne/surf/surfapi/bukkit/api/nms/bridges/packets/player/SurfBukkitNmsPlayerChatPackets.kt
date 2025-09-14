package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.player

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.core.api.util.requiredService
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import java.time.Instant
import java.util.*

typealias LastSeenMessagesPacked = Map<Int, SignedMessage.Signature?>

@NmsUseWithCaution
interface SurfBukkitNmsPlayerChatPackets {

    fun sendPlayerChatMessagePacket(
        senderUuid: UUID,
        senderDisplayName: Component,
        globalIndex: Int,
        index: Int,
        signature: SignedMessage.Signature?,
        salt: Long,
        timestamp: Instant,
        content: String,
        unsignedContent: Component?,
        lastSeen: LastSeenMessagesPacked,
    ): PacketOperation

    companion object {
        val instance = requiredService<SurfBukkitNmsPlayerChatPackets>()
    }
}

@NmsUseWithCaution
val nmsPlayerChatPacketsBridge get() = SurfBukkitNmsPlayerChatPackets.instance