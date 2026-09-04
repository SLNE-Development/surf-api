package dev.slne.surf.api.minestom.chat.signature

import com.google.common.primitives.Ints
import dev.slne.surf.api.minestom.util.NIL_UUID
import dev.slne.surf.api.minestom.util.toByteArray
import java.util.UUID

data class SignedMessageLink(
    val index: Int,
    val sender: UUID,
    val sessionId: UUID
) {

    companion object {
        fun unsigned(sender: UUID) = root(sender, NIL_UUID)

        fun root(sender: UUID, sessionId: UUID) = SignedMessageLink(0, sender, sessionId)
    }

    fun isDescendantOf(link: SignedMessageLink): Boolean =
        index > link.index && sender == link.sender && sessionId == link.sessionId

    fun advance(): SignedMessageLink? =
        if (index == Int.MAX_VALUE) null else copy(index = index + 1)

    fun updateSignature(output: SignatureUpdater.Output) {
        output.update(sender.toByteArray())
        output.update(sessionId.toByteArray())
        output.update(Ints.toByteArray(index))
    }
}
