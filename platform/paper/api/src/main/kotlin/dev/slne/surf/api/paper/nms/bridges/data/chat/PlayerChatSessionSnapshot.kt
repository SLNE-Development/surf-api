package dev.slne.surf.api.paper.nms.bridges.data.chat

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import kotlinx.serialization.Serializable

@NmsUseWithCaution
@Serializable
data class PlayerChatSessionSnapshot(
    val nextChatIndex: Int,
    val chatSession: RemoteChatSessionData?,
    val lastSeenMessages: LastSeenMessagesValidatorMirror,
    val messageSignatureCache: Array<ByteArray?>,
    val incomingChatChain: IncomingChatChainMirror
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayerChatSessionSnapshot) return false

        if (nextChatIndex != other.nextChatIndex) return false
        if (chatSession != other.chatSession) return false
        if (lastSeenMessages != other.lastSeenMessages) return false
        if (!messageSignatureCache.contentDeepEquals(other.messageSignatureCache)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = nextChatIndex
        result = 31 * result + chatSession.hashCode()
        result = 31 * result + lastSeenMessages.hashCode()
        result = 31 * result + messageSignatureCache.contentDeepHashCode()
        return result
    }
}