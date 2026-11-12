@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.nms.bridges.packets.player

import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.shared.api.util.InternalSurfApi
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

    companion object {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge by surfBean<SurfPaperNmsPlayerChatPackets>()
