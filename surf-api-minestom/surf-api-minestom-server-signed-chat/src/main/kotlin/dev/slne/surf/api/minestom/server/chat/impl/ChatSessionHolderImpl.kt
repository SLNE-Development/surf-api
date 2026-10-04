package dev.slne.surf.api.minestom.server.chat.impl

import dev.slne.surf.api.minestom.chat.ChatSessionHolder
import dev.slne.surf.api.minestom.chat.RemoteChatSender
import dev.slne.surf.api.minestom.chat.RemoteSignedMessage
import dev.slne.surf.api.minestom.extension.ConnectionManager
import dev.slne.surf.api.minestom.server.chat.SignedChatSettings
import dev.slne.surf.api.minestom.server.chat.impl.packet.DeleteChatPacketModern
import dev.slne.surf.api.minestom.server.chat.impl.packet.framed
import dev.slne.surf.api.minestom.server.chat.impl.signature.FILTER_MASK_PASS_THROUGH
import dev.slne.surf.api.minestom.server.chat.impl.signature.LastSeenMessages
import dev.slne.surf.api.minestom.server.chat.impl.signature.PlayerChatMessage
import dev.slne.surf.api.minestom.server.chat.impl.signature.SignedMessageBody
import dev.slne.surf.api.minestom.server.chat.impl.signature.SignedMessageLink
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minestom.server.crypto.ChatSession
import net.minestom.server.crypto.MessageSignature
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.network.packet.server.play.PlayerInfoRemovePacket
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * The secure chat state of one player while `withSignedChat()` is installed.
 */
internal class ChatSessionHolderImpl(
    override val player: Player,
    val handler: PlayerChatHandler,
) : ChatSessionHolder {

    override val chatSession: ChatSession?
        get() = handler.chatSession?.asData()

    override fun sendSignedMessage(
        message: SignedMessage,
        boundName: Component,
        unsignedContent: Component?,
    ) {
        val signed = (message as? PlayerChatMessage.AdventureView)?.playerChatMessage
        if (signed == null) {
            player.sendMessage(unsignedContent ?: Component.text(message.message()))
            return
        }

        OutgoingChatMessage.create(signed).sendToPlayer(
            handler = handler,
            filtered = false,
            chatType = BoundChatType(SurfChatTypes.raw, boundName),
            unsigned = unsignedContent,
        )
    }

    override fun captureSignedMessage(
        message: SignedMessage,
        unsignedContent: Component?,
    ): RemoteSignedMessage? {
        val signed = (message as? PlayerChatMessage.AdventureView)?.playerChatMessage ?: return null

        return RemoteSignedMessage(
            sender = signed.link.sender,
            sessionId = signed.link.sessionId,
            index = signed.link.index,
            signature = signed.signature,
            content = signed.signedBody.content,
            timestamp = signed.signedBody.timeStamp,
            salt = signed.signedBody.salt,
            lastSeen = signed.signedBody.lastSeen.entries,
            unsignedContent = unsignedContent
        )
    }

    override fun sendRemoteSignedMessage(
        sender: RemoteChatSender,
        message: RemoteSignedMessage,
        boundName: Component,
    ) {
        val signed = PlayerChatMessage(
            link = SignedMessageLink(message.index, message.sender, message.sessionId),
            signature = message.signature,
            signedBody = SignedMessageBody(
                message.content,
                message.timestamp,
                message.salt,
                LastSeenMessages(message.lastSeen)
            ),
            unsignedContent = message.unsignedContent,
            filterMask = FILTER_MASK_PASS_THROUGH
        )

        val announce = ConnectionManager.getOnlinePlayerByUuid(sender.uuid) == null
        if (announce) player.sendPacket(remoteSenderInfoPacket(sender))

        handler.sendPlayerChatMessage(signed, BoundChatType(SurfChatTypes.raw, boundName))

        if (announce) player.sendPacket(PlayerInfoRemovePacket(sender.uuid))
    }

    override fun deleteMessage(signature: SignedMessage.Signature) = deleteMessage(player, signature)

    private fun remoteSenderInfoPacket(sender: RemoteChatSender): PlayerInfoUpdatePacket {
        val actions = EnumSet.of(PlayerInfoUpdatePacket.Action.ADD_PLAYER)
        if (sender.session != null) actions.add(PlayerInfoUpdatePacket.Action.INITIALIZE_CHAT)

        return PlayerInfoUpdatePacket(
            actions,
            PlayerInfoUpdatePacket.Entry(
                sender.uuid,
                sender.username,
                emptyList(),
                false,
                0,
                GameMode.ADVENTURE,
                null,
                sender.session,
                0,
                false
            )
        )
    }

    @OptIn(InternalSurfApi::class)
    companion object : ChatSessionHolder.Provider {
        private val holders = ConcurrentHashMap<UUID, ChatSessionHolderImpl>()

        override fun fromPlayer(player: Player): ChatSessionHolderImpl? = holders[player.uuid]
            ?.takeIf { it.player === player }

        override fun fromUuid(uuid: UUID): ChatSessionHolderImpl? = holders[uuid]

        override fun deleteMessage(player: Player, signature: SignedMessage.Signature) {
            val packed = MessageSignature.Packed(MessageSignature(signature.bytes()))
            player.sendPacket(DeleteChatPacketModern(packed).framed())
        }

        fun create(player: Player, settings: SignedChatSettings): ChatSessionHolderImpl? {
            if (!player.isOnline) return null

            return holders.compute(player.uuid) { _, existing ->
                existing?.takeIf { it.player === player }
                    ?: ChatSessionHolderImpl(player, PlayerChatHandler(player, settings))
            }
        }

        fun remove(player: Player): ChatSessionHolderImpl? {
            val holder = holders[player.uuid]?.takeIf { it.player === player } ?: return null
            holders.remove(player.uuid, holder)
            return holder
        }

        fun clear() {
            holders.values.forEach { holder -> holder.handler.close() }
            holders.clear()
        }
    }
}
