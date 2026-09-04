package dev.slne.surf.api.minestom.chat.signature

import com.google.common.primitives.Ints
import dev.slne.surf.api.minestom.util.NIL_UUID
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.minestom.server.crypto.FilterMask
import net.minestom.server.crypto.MessageSignature
import java.time.Duration
import java.time.Instant
import java.util.UUID


data class PlayerChatMessage(
    val link: SignedMessageLink,
    val signature: MessageSignature?,
    val signedBody: SignedMessageBody,
    val unsignedContent: Component?,
    val filterMask: FilterMask
) {
    companion object {
        private val SYSTEM_SENDER = NIL_UUID

        val MESSAGE_EXPIRES_AFTER_SERVER: Duration = Duration.ofMinutes(5L)
        val MESSAGE_EXPIRES_AFTER_CLIENT: Duration =
            MESSAGE_EXPIRES_AFTER_SERVER.plus(Duration.ofMinutes(2L))

        fun system(content: String) = unsigned(SYSTEM_SENDER, content)

        fun unsigned(sender: UUID, content: String) = PlayerChatMessage(
            link = SignedMessageLink.unsigned(sender),
            signature = null,
            signedBody = SignedMessageBody.unsigned(content),
            unsignedContent = null,
            filterMask = FILTER_MASK_PASS_THROUGH
        )

        fun updateSignature(
            output: SignatureUpdater.Output,
            link: SignedMessageLink,
            body: SignedMessageBody
        ) {
            output.update(Ints.toByteArray(1))
            link.updateSignature(output)
            body.updateSignature(output)
        }
    }

    fun withUnsignedContent(content: Component) =
        copy(unsignedContent = if (content != Component.text(signedContent())) content else null)

    fun removeUnsignedContent() =
        if (unsignedContent != null) copy(unsignedContent = null) else this

    fun filter(filterMask: FilterMask) =
        if (this.filterMask == filterMask) this else copy(filterMask = filterMask)

    fun filter(filtered: Boolean) = filter(if (filtered) filterMask else FILTER_MASK_PASS_THROUGH)

    fun removeSignature() = PlayerChatMessage(
        link = SignedMessageLink.unsigned(sender()),
        signature = null,
        signedBody = SignedMessageBody.unsigned(signedContent()),
        unsignedContent = unsignedContent,
        filterMask = filterMask
    )

    fun verify(signatureValidator: SignatureValidator): Boolean =
        signature != null && signature.verify(signatureValidator) { output ->
            updateSignature(output, link, signedBody)
        }

    fun signedContent() = signedBody.content

    fun decoratedContent(): Component = unsignedContent ?: Component.text(signedContent())

    fun timeStamp() = signedBody.timeStamp

    fun salt() = signedBody.salt

    fun hasExpiredServer(now: Instant): Boolean =
        now.isAfter(timeStamp().plus(MESSAGE_EXPIRES_AFTER_SERVER))

    fun hasExpiredClient(now: Instant): Boolean =
        now.isAfter(timeStamp().plus(MESSAGE_EXPIRES_AFTER_CLIENT))

    fun sender() = link.sender

    fun isSystem() = sender() == SYSTEM_SENDER

    fun hasSignature() = signature != null

    fun hasSignatureFrom(profileId: UUID) = hasSignature() && link.sender == profileId

    fun isFullyFiltered() = filterMask.isFullyFiltered()

    fun describeSigned(): String = buildString {
        append("'").append(signedBody.content).append("' @ ").append(signedBody.timeStamp)
        append("\n - From: ").append(link.sender).append("/").append(link.sessionId)
        append(", message #").append(link.index)
        append("\n - Salt: ").append(signedBody.salt)
        append("\n - Signature: ").append(signature.describe())
        append("\n - Last Seen: [\n")
        for (entry in signedBody.lastSeen.entries) {
            append("     ").append(entry.describe()).append("\n")
        }
        append(" ]\n")
    }

    fun adventureView() = AdventureView()

    inner class AdventureView : SignedMessage {
        override fun timestamp(): Instant = this@PlayerChatMessage.timeStamp()

        override fun salt(): Long = this@PlayerChatMessage.salt()

        override fun signature(): SignedMessage.Signature? =
            this@PlayerChatMessage.signature?.adventure()

        override fun unsignedContent(): Component? = this@PlayerChatMessage.unsignedContent

        override fun message(): String = this@PlayerChatMessage.signedContent()

        override fun identity(): Identity = Identity.identity(sender())

        val playerChatMessage get() = this@PlayerChatMessage
    }
}
