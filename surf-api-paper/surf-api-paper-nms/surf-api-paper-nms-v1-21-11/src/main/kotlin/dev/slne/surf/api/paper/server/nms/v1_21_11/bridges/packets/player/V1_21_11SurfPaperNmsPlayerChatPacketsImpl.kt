package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.player

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.bridges.packets.player.LastSeenMessagesPacked
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerChatPackets
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.FilterMask
import net.minecraft.network.chat.LastSeenMessages
import net.minecraft.network.chat.SignedMessageBody
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket
import net.minecraft.server.MinecraftServer
import java.time.Instant
import java.util.*
import net.minecraft.network.chat.MessageSignature as NmsMessageSignature

@NmsUseWithCaution
class V1_21_11SurfPaperNmsPlayerChatPacketsImpl : SurfPaperNmsPlayerChatPackets {
    override fun sendPlayerChatMessagePacket(
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
    ): PacketOperation = V1_21_11PacketOperationImpl.simple { player ->
        val messageSignature = signature?.let { NmsMessageSignature(it.bytes()) }
        val signedBody = SignedMessageBody.Packed(content, timestamp, salt, lastSeen.toNms())

        ClientboundPlayerChatPacket(
            globalIndex,
            senderUuid,
            index,
            messageSignature,
            signedBody,
            unsignedContent?.toNms(),
            FilterMask.PASS_THROUGH,
            ChatType.bind(
                ChatType.CHAT,
                MinecraftServer.getServer().registryAccess(),
                senderDisplayName.toNms()
            )
        )
    }

    private fun LastSeenMessagesPacked.toNms(): LastSeenMessages.Packed {
        val list = mutableListOf<NmsMessageSignature.Packed>()

        for ((id, signature) in this) {
            val nmsSignature = signature?.let { NmsMessageSignature(it.bytes()) }
            list.add(NmsMessageSignature.Packed(id, nmsSignature))
        }

        return LastSeenMessages.Packed(list)
    }
}
