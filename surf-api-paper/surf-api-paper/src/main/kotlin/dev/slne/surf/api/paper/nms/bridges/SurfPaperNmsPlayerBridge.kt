package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.data.chat.PlayerChatMessageMirror
import dev.slne.surf.api.paper.nms.bridges.data.chat.RemoteChatSessionData
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.chat.ChatType
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus

@NmsUseWithCaution
@ApiStatus.NonExtendable
interface SurfPaperNmsPlayerBridge {

    fun getRemoteChatSessionData(player: Player): RemoteChatSessionData?

    fun runOnChatMessageChain(player: Player, scope: CoroutineScope, block: suspend () -> Unit)

    fun sendSignedMessageWithChangedContent(
        player: Player,
        original: SignedMessage,
        boundChatType: ChatType.Bound,
        unsignedContent: Component
    )

    fun increaseNextChatIndex(player: Player): Int?

    fun createPlayerChatMessageMirrorFromAdventure(
        adventure: SignedMessage,
        unsignedContent: Component? = adventure.unsignedContent()
    ): PlayerChatMessageMirror?

    fun createAdventureChatMessageFromMirror(mirror: PlayerChatMessageMirror): SignedMessage

    fun sendPlayerChatMessage(
        receiver: Player,
        message: SignedMessage,
        boundChatType: ChatType.Bound
    )

    fun getPaperRawChatType(): ChatType

    companion object : SurfPaperNmsPlayerBridge by playerBridge {
        val INSTANCE get() = playerBridge
    }
}

@NmsUseWithCaution
private val playerBridge = requiredService<SurfPaperNmsPlayerBridge>()