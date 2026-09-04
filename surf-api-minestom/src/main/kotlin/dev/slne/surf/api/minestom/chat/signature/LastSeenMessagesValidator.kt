package dev.slne.surf.api.minestom.chat.signature

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.minestom.server.crypto.MessageSignature
import java.io.Serial

class LastSeenMessagesValidator(
    private val lastSeenCount: Int = LastSeenMessages.LAST_SEEN_MESSAGES_MAX_LENGTH
) {

    private val trackedMessages = ObjectArrayList<LastSeenTrackedEntry?>().apply {
        repeat(lastSeenCount) { add(null) }
    }

    private var lastPendingMessage: MessageSignature? = null

    val trackedMessagesCount get() = trackedMessages.size

    fun addPending(message: MessageSignature) {
        if (message != lastPendingMessage) {
            trackedMessages.add(LastSeenTrackedEntry(message, true))
            lastPendingMessage = message
        }
    }

    fun applyOffset(offset: Int) {
        val maxOffset = trackedMessages.size - lastSeenCount
        if (offset !in 0..maxOffset) {
            throw ValidationException(
                "Advanced last seen window by $offset messages, but expected at most $maxOffset"
            )
        }

        trackedMessages.removeElements(0, offset)
    }

    fun applyUpdate(update: LastSeenMessages.Update): LastSeenMessages {
        applyOffset(update.offset)

        val lastSeenEntries = ObjectArrayList<MessageSignature>(update.acknowledged.cardinality())
        val ackLength = update.acknowledged.length()
        if (ackLength > lastSeenCount) {
            throw ValidationException(
                "Last seen update contained $ackLength messages, but maximum window size is $lastSeenCount"
            )
        }

        for (i in 0 until lastSeenCount) {
            val acknowledged = update.acknowledged.get(i)
            val message = trackedMessages[i]

            if (acknowledged) {
                if (message == null) {
                    throw ValidationException(
                        "Last seen update acknowledged unknown or previously ignored message at index $i"
                    )
                }

                trackedMessages[i] = message.acknowledge()
                lastSeenEntries.add(message.signature)
            } else {
                if (message != null && !message.pending) {
                    throw ValidationException(
                        "Last seen update ignored previously acknowledged message at index $i and signature ${message.signature}"
                    )
                }

                trackedMessages[i] = null
            }
        }

        val lastSeen = LastSeenMessages(lastSeenEntries)
        if (!update.verifyChecksum(lastSeen)) {
            throw ValidationException(
                "Checksum mismatch on last seen update: the client and server must have desynced"
            )
        }

        return lastSeen
    }

    class ValidationException(message: String) : Exception(message, null, false, false) {
        companion object {
            @Serial
            private const val serialVersionUID: Long = -5942795035956998806L
        }
    }
}
