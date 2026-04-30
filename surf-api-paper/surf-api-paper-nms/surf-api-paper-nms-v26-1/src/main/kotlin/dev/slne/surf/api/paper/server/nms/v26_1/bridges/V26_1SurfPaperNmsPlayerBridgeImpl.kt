package dev.slne.surf.api.paper.server.nms.v26_1.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge
import dev.slne.surf.api.paper.nms.bridges.data.chat.PlayerChatMessageMirror
import dev.slne.surf.api.paper.nms.bridges.data.chat.RemoteChatSessionData
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_1.reflection.NmsReflections
import io.papermc.paper.adventure.PaperAdventure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.chat.ChatType
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minecraft.network.chat.*
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

@NmsUseWithCaution
class V26_1SurfPaperNmsPlayerBridgeImpl : SurfPaperNmsPlayerBridge {
    override fun getRemoteChatSessionData(player: Player): RemoteChatSessionData? {
        val session = player.toNms().chatSession?.asData() ?: return null
        val profilePublicKey = session.profilePublicKey()

        return RemoteChatSessionData(
            sessionId = session.sessionId(),
            expiresAt = profilePublicKey.expiresAt(),
            key = profilePublicKey.key(),
            keySignature = profilePublicKey.keySignature()
        )
    }

    override fun runOnChatMessageChain(player: Player, scope: CoroutineScope, block: suspend () -> Unit) {
        val nmsPlayer = player.toNms()
        val connection = nmsPlayer.connection
        val chatMessageChain = NmsReflections.getChatMessageChain(connection)
        val done = CompletableFuture<Unit>()

        synchronized(chatMessageChain) {
            chatMessageChain.append {
                scope.launch {
                    try {
                        block()
                        done.complete(Unit)
                    } catch (e: Throwable) {
                        done.completeExceptionally(e)
                    }
                }
            }

            chatMessageChain.append(done) { /* no-op */ }
        }
    }

    override fun sendSignedMessageWithChangedContent(
        player: Player,
        original: SignedMessage,
        boundChatType: ChatType.Bound,
        unsignedContent: Component
    ) {
        if (original !is PlayerChatMessage.AdventureView) {
            if (original.isSystem) {
                player.sendMessage(unsignedContent, boundChatType)
            } else {
                player.sendMessage(unsignedContent, boundChatType)
            }
            return
        }
        val nmsPlayer = player.toNms()

        nmsPlayer.sendChatMessage(
            OutgoingChatMessage.create(original.playerChatMessage()),
            nmsPlayer.isTextFilteringEnabled,
            boundChatType.toNms(nmsPlayer),
            PaperAdventure.asVanilla(unsignedContent)
        )
    }

    @Suppress("USELESS_ELVIS")
    override fun increaseNextChatIndex(player: Player): Int? {
        val connection = player.toNms().connection ?: return null
        return NmsReflections.increaseAndGetNextChatIndex(connection)
    }

    override fun createPlayerChatMessageMirrorFromAdventure(
        adventure: SignedMessage,
        unsignedContent: Component?
    ): PlayerChatMessageMirror? {
        if (adventure !is PlayerChatMessage.AdventureView) return null
        val nms = adventure.playerChatMessage()
        val nmsLink = nms.link
        val nmsBody = nms.signedBody()
        val nmsLastSeen = nmsBody.lastSeen()

        val link = PlayerChatMessageMirror.SignedMessageLink(nmsLink.index(), nmsLink.sender(), nmsLink.sessionId())
        val lastSeen = PlayerChatMessageMirror.SignedMessageBody.LastSeenMessages(
            nmsLastSeen.entries().map { it.bytes() }
        )
        val body = PlayerChatMessageMirror.SignedMessageBody(
            nmsBody.content(),
            nmsBody.timeStamp(),
            nmsBody.salt(),
            lastSeen
        )

        val filterMask = when (val mask = nms.filterMask()) {
            FilterMask.FULLY_FILTERED -> PlayerChatMessageMirror.FilterMask.FULLY_FILTERED
            FilterMask.PASS_THROUGH -> PlayerChatMessageMirror.FilterMask.PASS_THROUGH
            else -> PlayerChatMessageMirror.FilterMask(NmsReflections.getMask(mask))
        }

        return PlayerChatMessageMirror(
            link = link,
            signature = nms.signature()?.bytes(),
            unsignedContent = unsignedContent,
            signedBody = body,
            filterMask = filterMask,
        )
    }

    override fun createAdventureChatMessageFromMirror(mirror: PlayerChatMessageMirror): SignedMessage {
        val mirrorLink = mirror.link
        val mirrorBody = mirror.signedBody
        val mirrorLastSeen = mirrorBody.lastSeen

        val link = SignedMessageLink(mirrorLink.index, mirrorLink.sender, mirrorLink.sessionId)
        val lastSeen = LastSeenMessages(mirrorLastSeen.entries.map(::MessageSignature))
        val body = SignedMessageBody(mirrorBody.content, mirrorBody.timestamp, mirrorBody.salt, lastSeen)

        val filterMask = when (mirror.filterMask) {
            PlayerChatMessageMirror.FilterMask.FULLY_FILTERED -> FilterMask.FULLY_FILTERED
            PlayerChatMessageMirror.FilterMask.PASS_THROUGH -> FilterMask.PASS_THROUGH
            else -> NmsReflections.createFilterMask(mirror.filterMask.mask)
        }

        return PlayerChatMessage(
            link,
            mirror.signature?.let(::MessageSignature),
            body,
            PaperAdventure.asVanilla(mirror.unsignedContent),
            filterMask
        ).adventureView()
    }

    @Suppress("USELESS_ELVIS")
    override fun sendPlayerChatMessage(receiver: Player, message: SignedMessage, boundChatType: ChatType.Bound) {
        require(message is PlayerChatMessage.AdventureView) { "Only native nms messages can be sent" }
        val nmsMessage = message.playerChatMessage()
        val nmsPlayer = receiver.toNms()
        val connection = nmsPlayer.connection ?: return
        val messageSignatureCache = NmsReflections.getMessageSignatureCache(connection)

        synchronized(messageSignatureCache) {
            connection.send(
                ClientboundPlayerChatPacket(
                    NmsReflections.increaseAndGetNextChatIndex(connection),
                    nmsMessage.link().sender(),
                    nmsMessage.link().index(),
                    nmsMessage.signature(),
                    nmsMessage.signedBody().pack(messageSignatureCache),
                    nmsMessage.unsignedContent(),
                    nmsMessage.filterMask(),
                    boundChatType.toNms(nmsPlayer)
                )
            )

            val signature = nmsMessage.signature()
            if (signature != null) {
                messageSignatureCache.push(nmsMessage.signedBody(), signature)
                val lastSeenMessages = NmsReflections.getLastSeenMessages(connection)

                synchronized(lastSeenMessages) {
                    lastSeenMessages.addPending(signature)
                }
            }
        }
    }
}