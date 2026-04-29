package dev.slne.surf.api.paper.server.nms.v26_1.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge
import dev.slne.surf.api.paper.nms.bridges.data.chat.RemoteChatSessionData
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
import io.papermc.paper.adventure.PaperAdventure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.kyori.adventure.chat.ChatType
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minecraft.network.chat.OutgoingChatMessage
import net.minecraft.network.chat.PlayerChatMessage
import net.minecraft.server.network.ServerGamePacketListenerImpl
import net.minecraft.util.FutureChain
import org.bukkit.entity.Player
import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
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
        val chatMessageChain = `serverGamePacketListenerImpl$chatMessageChain`.get(connection) as FutureChain
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

    @Suppress("ObjectPrivatePropertyName")
    companion object {
        @JvmStatic
        private val `serverGamePacketListenerImpl$chatMessageChain`: VarHandle

        init {
            val lookup = MethodHandles.lookup()
            val privateLookupInServerGamePacketListener =
                MethodHandles.privateLookupIn(ServerGamePacketListenerImpl::class.java, lookup)

            `serverGamePacketListenerImpl$chatMessageChain` = privateLookupInServerGamePacketListener
                .findVarHandle(
                    ServerGamePacketListenerImpl::class.java,
                    "chatMessageChain",
                    FutureChain::class.java
                )
        }
    }
}