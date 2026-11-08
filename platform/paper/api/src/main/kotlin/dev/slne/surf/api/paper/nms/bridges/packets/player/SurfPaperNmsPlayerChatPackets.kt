package dev.slne.surf.api.paper.nms.bridges.packets.player

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import java.time.Instant
import java.util.*

typealias LastSeenMessagesPacked = Map<Int, SignedMessage.Signature?>

@NmsUseWithCaution
interface SurfPaperNmsPlayerChatPackets {

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

    companion object : SurfPaperNmsPlayerChatPackets by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsPlayerChatPackets>()