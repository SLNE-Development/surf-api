package dev.slne.surf.api.minestom.chat.signature

import net.minestom.server.crypto.ChatSession
import net.minestom.server.crypto.PlayerPublicKey
import java.util.UUID
import net.minestom.server.crypto.SignatureValidator as ServiceSignatureValidator

data class RemoteChatSession(
    val sessionId: UUID,
    val profilePublicKey: PlayerPublicKey
) {

    companion object {
        fun validate(
            data: ChatSession,
            profileId: UUID,
            serviceSignatureValidator: ServiceSignatureValidator
        ) = RemoteChatSession(
            sessionId = data.sessionId(),
            profilePublicKey = data.publicKey().createValidated(serviceSignatureValidator, profileId)
        )
    }

    fun createMessageDecoder(profileId: UUID): SignedMessageChain.Decoder =
        SignedMessageChain(profileId, sessionId).decoder(profilePublicKey)

    fun hasExpired() = profilePublicKey.hasExpired()

    fun asData(): ChatSession = ChatSession(sessionId, profilePublicKey)
}
