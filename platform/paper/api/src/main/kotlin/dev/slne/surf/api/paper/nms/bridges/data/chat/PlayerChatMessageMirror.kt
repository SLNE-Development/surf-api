package dev.slne.surf.api.paper.nms.bridges.data.chat

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import java.time.Instant
import java.util.*

@Serializable
@NmsUseWithCaution
data class PlayerChatMessageMirror(
    val link: SignedMessageLink,
    val signature: ByteArray?,
    val signedBody: SignedMessageBody,
    val unsignedContent: @Contextual Component?,
    val filterMask: FilterMask,
) {

    @Serializable
    data class FilterMask(val mask: @Contextual BitSet, val type: TYPE = TYPE.PARTIALLY_FILTERED) {
        companion object {
            val FULLY_FILTERED = FilterMask(BitSet(0), TYPE.FULLY_FILTERED)
            val PASS_THROUGH = FilterMask(BitSet(0), TYPE.PASS_THROUGH)
        }

        enum class TYPE {
            PASS_THROUGH,
            FULLY_FILTERED,
            PARTIALLY_FILTERED
        }
    }

    @Serializable
    data class SignedMessageLink(val index: Int, val sender: @Contextual UUID, val sessionId: @Contextual UUID)

    @Serializable
    data class SignedMessageBody(
        val content: String,
        val timestamp: @Contextual Instant,
        val salt: Long,
        val lastSeen: LastSeenMessages,
    ) {

        @Serializable
        data class LastSeenMessages(val entries: List<ByteArray>)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PlayerChatMessageMirror) return false

        if (link != other.link) return false
        if (!signature.contentEquals(other.signature)) return false
        if (signedBody != other.signedBody) return false
        if (unsignedContent != other.unsignedContent) return false
        if (filterMask != other.filterMask) return false

        return true
    }

    override fun hashCode(): Int {
        var result = link.hashCode()
        result = 31 * result + (signature?.contentHashCode() ?: 0)
        result = 31 * result + signedBody.hashCode()
        result = 31 * result + (unsignedContent?.hashCode() ?: 0)
        result = 31 * result + filterMask.hashCode()
        return result
    }
}