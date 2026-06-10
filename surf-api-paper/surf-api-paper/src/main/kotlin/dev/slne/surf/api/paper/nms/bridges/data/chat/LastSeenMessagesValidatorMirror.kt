package dev.slne.surf.api.paper.nms.bridges.data.chat

import kotlinx.serialization.Serializable

@Serializable
data class LastSeenMessagesValidatorMirror(
    val lastSeenCount: Int,
    val trackedMessages: List<LastSeenTrackedEntry>,
    val lastPendingMessage: ByteArray? = null,
) {

    @Serializable
    data class LastSeenTrackedEntry(
        val signature: ByteArray,
        val pending: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LastSeenTrackedEntry) return false

            if (pending != other.pending) return false
            if (!signature.contentEquals(other.signature)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = pending.hashCode()
            result = 31 * result + signature.contentHashCode()
            return result
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LastSeenMessagesValidatorMirror) return false

        if (lastSeenCount != other.lastSeenCount) return false
        if (trackedMessages != other.trackedMessages) return false
        if (!lastPendingMessage.contentEquals(other.lastPendingMessage)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = lastSeenCount
        result = 31 * result + trackedMessages.hashCode()
        result = 31 * result + (lastPendingMessage?.contentHashCode() ?: 0)
        return result
    }
}