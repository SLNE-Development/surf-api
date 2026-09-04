package dev.slne.surf.api.minestom.chat.signature

import com.google.common.primitives.Ints
import com.google.common.primitives.Longs
import java.time.Instant
import net.minestom.server.crypto.SignedMessageBody as NetworkSignedMessageBody


data class SignedMessageBody(
    val content: String,
    val timeStamp: Instant,
    val salt: Long,
    val lastSeen: LastSeenMessages
) {

    companion object {
        fun unsigned(content: String) =
            SignedMessageBody(content, Instant.now(), 0L, LastSeenMessages.EMPTY)
    }

    fun updateSignature(output: SignatureUpdater.Output) {
        output.update(Longs.toByteArray(salt))
        output.update(Longs.toByteArray(timeStamp.epochSecond))

        val contentBytes = content.toByteArray()
        output.update(Ints.toByteArray(contentBytes.size))
        output.update(contentBytes)

        lastSeen.updateSignature(output)
    }

    fun pack(cache: MessageSignatureCache): NetworkSignedMessageBody.Packed =
        NetworkSignedMessageBody.Packed(content, timeStamp, salt, lastSeen.pack(cache))
}
