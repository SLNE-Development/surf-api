package dev.slne.surf.api.paper.nms.bridges.data.chat

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.Instant

@NmsUseWithCaution
@Serializable
data class IncomingChatChainMirror(
    val nextLinkIndex: Int?,
    val lastTimeStamp: @Contextual Instant,
)