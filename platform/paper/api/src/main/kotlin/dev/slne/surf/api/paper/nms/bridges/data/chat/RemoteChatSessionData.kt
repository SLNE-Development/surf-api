package dev.slne.surf.api.paper.nms.bridges.data.chat

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.security.PublicKey
import java.time.Instant
import java.util.*

@NmsUseWithCaution
@Serializable
data class RemoteChatSessionData(
    val sessionId: @Contextual UUID,
    val expiresAt: @Contextual Instant,
    val key: @Contextual PublicKey,
    val keySignature: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteChatSessionData) return false

        if (sessionId != other.sessionId) return false
        if (expiresAt != other.expiresAt) return false
        if (key != other.key) return false
        if (!keySignature.contentEquals(other.keySignature)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sessionId.hashCode()
        result = 31 * result + expiresAt.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + keySignature.contentHashCode()
        return result
    }
}