package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge
import dev.slne.surf.api.paper.nms.bridges.data.chat.RemoteChatSessionData
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import org.bukkit.entity.Player

@NmsUseWithCaution
class V1_21_11SurfPaperNmsPlayerBridgeImpl : SurfPaperNmsPlayerBridge {
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
}