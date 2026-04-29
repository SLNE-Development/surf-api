package dev.slne.surf.api.paper.server.nms.v26_1.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge
import dev.slne.surf.api.paper.nms.bridges.data.chat.RemoteChatSessionData
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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

    @Suppress("ObjectPrivatePropertyName")
    companion object {
        @JvmStatic
        private val `serverGamePacketListenerImpl$chatMessageChain`: VarHandle

        init {
            val lookup = MethodHandles.lookup()
            `serverGamePacketListenerImpl$chatMessageChain` = lookup.findVarHandle(
                ServerGamePacketListenerImpl::class.java,
                "chatMessageChain",
                FutureChain::class.java
            )
        }
    }
}