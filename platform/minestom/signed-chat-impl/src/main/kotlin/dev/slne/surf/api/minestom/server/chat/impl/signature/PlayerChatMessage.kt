package dev.slne.surf.api.minestom.server.chat.impl.signature

import com.google.common.primitives.Ints
import dev.slne.surf.api.minestom.server.chat.impl.NIL_UUID
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.minestom.server.crypto.FilterMask
import net.minestom.server.crypto.MessageSignature
import java.time.Duration
import java.time.Instant
import java.util.*

internal data class PlayerChatMessage(
    val link: SignedMessageLink,
    val signature: MessageSignature?,
    val signedBody: SignedMessageBody,
    val unsignedContent: Component?,
    val filterMask: FilterMask,
) {
    companion object {
        private val SYSTEM_SENDER = NIL_UUID

        val MESSAGE_EXPIRES_AFTER_SERVER: Duration = Duration.ofMinutes(5L)

        fun system(content: String): PlayerChatMessage = unsigned(SYSTEM_SENDER, content)

        fun unsigned(sender: UUID, content: String): PlayerChatMessage {
            return PlayerChatMessage(
                link = SignedMessageLink.unsigned(sender),
                signature = null,
                signedBody = SignedMessageBody.unsigned(content),
                unsignedContent = null,
                filterMask = FILTER_MASK_PASS_THROUGH,
            )
        }

        fun updateSignature(
            output: SignatureUpdater.Output,
            link: SignedMessageLink,
            body: SignedMessageBody,
        ) {
            output.update(Ints.toByteArray(1))
            link.updateSignature(output)
            body.updateSignature(output)
        }
    }

    fun withUnsignedContent(content: Component): PlayerChatMessage {
        return copy(
            unsignedContent = if (content != Component.text(
                    signedContent(),
                )
            ) content else null,
        )
    }

    fun verify(signatureValidator: SignatureValidator): Boolean {
        return signature != null && signature.verify(signatureValidator) { output ->
            updateSignature(output, link, signedBody)
        }
    }

    fun signedContent(): String = signedBody.content

    fun decoratedContent(): Component = unsignedContent ?: Component.text(signedContent())

    fun timeStamp(): Instant = signedBody.timeStamp

    fun salt(): Long = signedBody.salt

    fun hasExpiredServer(now: Instant): Boolean {
        return now.isAfter(timeStamp().plus(MESSAGE_EXPIRES_AFTER_SERVER))
    }

    fun sender(): UUID = link.sender

    fun isSystem(): Boolean = sender() == SYSTEM_SENDER

    fun adventureView(): PlayerChatMessage.AdventureView = AdventureView()

    inner class AdventureView : SignedMessage {
        override fun timestamp(): Instant = this@PlayerChatMessage.timeStamp()

        override fun salt(): Long = this@PlayerChatMessage.salt()

        override fun signature(): SignedMessage.Signature? {
            return this@PlayerChatMessage.signature?.adventure()
        }

        override fun unsignedContent(): Component? = this@PlayerChatMessage.unsignedContent

        override fun message(): String = this@PlayerChatMessage.signedContent()

        override fun identity(): Identity = Identity.identity(sender())

        val playerChatMessage get() = this@PlayerChatMessage
    }
}
