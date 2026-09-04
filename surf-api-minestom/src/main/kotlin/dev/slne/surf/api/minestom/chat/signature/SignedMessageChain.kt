package dev.slne.surf.api.minestom.chat.signature

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.translatable
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.crypto.MessageSignature
import net.minestom.server.crypto.PlayerPublicKey
import java.io.Serial
import java.time.Instant
import java.util.UUID
import java.util.function.BooleanSupplier

class SignedMessageChain(profileId: UUID, sessionId: UUID) {

    companion object {
        private val LOGGER = ComponentLogger.logger()
    }

    private var nextLink: SignedMessageLink? = SignedMessageLink.root(profileId, sessionId)
    private var lastTimeStamp: Instant = Instant.EPOCH

    fun encoder(signer: Signer) = Encoder { body ->
        val link = nextLink ?: return@Encoder null
        nextLink = link.advance()

        MessageSignature(signer.sign { output ->
            PlayerChatMessage.updateSignature(output, link, body)
        })
    }

    fun decoder(profilePublicKey: PlayerPublicKey): Decoder {
        val signatureValidator = profilePublicKey.createSignatureValidator()

        return object : Decoder {
            override fun unpack(
                signature: MessageSignature?,
                body: SignedMessageBody
            ): PlayerChatMessage {
                if (signature == null) {
                    throw DecodeException(DecodeException.MISSING_PROFILE_KEY)
                }

                if (profilePublicKey.hasExpired()) {
                    throw DecodeException(DecodeException.EXPIRED_PROFILE_KEY)
                }

                val link = nextLink ?: throw DecodeException(DecodeException.CHAIN_BROKEN)

                if (body.timeStamp.isBefore(lastTimeStamp)) {
                    setChainBroken()
                    throw DecodeException(DecodeException.OUT_OF_ORDER_CHAT)
                }

                lastTimeStamp = body.timeStamp

                val unpacked = PlayerChatMessage(
                    link = link,
                    signature = signature,
                    signedBody = body,
                    unsignedContent = null,
                    filterMask = FILTER_MASK_PASS_THROUGH
                )

                if (!unpacked.verify(signatureValidator)) {
                    setChainBroken()
                    throw DecodeException(DecodeException.INVALID_SIGNATURE)
                }

                if (unpacked.hasExpiredServer(Instant.now())) {
                    LOGGER.warn(
                        "Received expired chat: '{}'. Is the client/server system time unsynchronized?",
                        body.content
                    )
                }

                nextLink = link.advance()
                return unpacked
            }

            override fun setChainBroken() {
                nextLink = null
            }
        }
    }

    class DecodeException(val component: Component) : Exception(null, null, false, false) {
        companion object {
            @Serial
            private const val serialVersionUID: Long = 6432477898999865178L

            val MISSING_PROFILE_KEY: Component = translatable("chat.disabled.missingProfileKey")
            val CHAIN_BROKEN: Component = translatable("chat.disabled.chain_broken")
            val EXPIRED_PROFILE_KEY: Component = translatable("chat.disabled.expiredProfileKey")
            val INVALID_SIGNATURE: Component = translatable("chat.disabled.invalid_signature")
            val OUT_OF_ORDER_CHAT: Component = translatable("chat.disabled.out_of_order_chat")
        }
    }

    fun interface Decoder {
        fun unpack(signature: MessageSignature?, body: SignedMessageBody): PlayerChatMessage

        fun setChainBroken() {}

        companion object {
            fun unsigned(profileId: UUID, enforcesSecureChat: BooleanSupplier) =
                Decoder { _, body ->
                    if (enforcesSecureChat.asBoolean) {
                        throw DecodeException(DecodeException.MISSING_PROFILE_KEY)
                    }

                    PlayerChatMessage.unsigned(profileId, body.content)
                }
        }
    }

    fun interface Encoder {
        fun pack(body: SignedMessageBody): MessageSignature?

        companion object {
            val UNSIGNED = Encoder { null }
        }
    }
}
