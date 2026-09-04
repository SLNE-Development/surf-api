package dev.slne.surf.api.minestom.chat.signature

import com.google.common.primitives.Ints
import dev.slne.surf.api.minestom.chat.signature.LastSeenMessages.Update.Companion.IGNORE_CHECKSUM
import net.minestom.server.crypto.MessageSignature
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket
import net.minestom.server.network.packet.client.play.ClientSignedCommandChatPacket
import java.util.BitSet
import net.minestom.server.crypto.LastSeenMessages as NetworkLastSeenMessages

@JvmInline
value class LastSeenMessages(val entries: List<MessageSignature>) {

    companion object {
        const val LAST_SEEN_MESSAGES_MAX_LENGTH = 20

        val EMPTY = LastSeenMessages(emptyList())
    }

    fun computeChecksum(): Byte {
        var checksum = 1

        for (entry in entries) {
            checksum = 31 * checksum + entry.checksum()
        }

        val checksumByte = checksum.toByte()

        return if (checksumByte == IGNORE_CHECKSUM) 1 else checksumByte
    }

    fun updateSignature(output: SignatureUpdater.Output) {
        output.update(Ints.toByteArray(entries.size))

        for (entry in entries) {
            output.update(entry.signature())
        }
    }

    fun pack(cache: MessageSignatureCache): NetworkLastSeenMessages.Packed =
        NetworkLastSeenMessages.Packed(entries.map { it.pack(cache) })

    data class Update(
        val offset: Int,
        val acknowledged: BitSet,
        val checksum: Byte
    ) {
        companion object {
            const val IGNORE_CHECKSUM = 0.toByte()

            fun fromPacket(packet: ClientChatMessagePacket) = Update(
                offset = packet.ackOffset(),
                acknowledged = packet.ackList(),
                checksum = packet.checksum()
            )

            fun fromPacket(packet: ClientSignedCommandChatPacket) = Update(
                offset = packet.lastSeenMessages().offset(),
                acknowledged = packet.lastSeenMessages().acknowledged(),
                checksum = packet.checksum()
            )
        }

        fun verifyChecksum(lastSeen: LastSeenMessages): Boolean =
            checksum == IGNORE_CHECKSUM || checksum == lastSeen.computeChecksum()
    }
}
