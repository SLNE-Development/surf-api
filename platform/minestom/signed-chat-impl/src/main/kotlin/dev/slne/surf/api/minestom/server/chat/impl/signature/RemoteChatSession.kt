package dev.slne.surf.api.minestom.server.chat.impl.signature

import net.minestom.server.crypto.ChatSession
import net.minestom.server.crypto.PlayerPublicKey
import java.util.*
import net.minestom.server.crypto.SignatureValidator as ServiceSignatureValidator

internal data class RemoteChatSession(
    val sessionId: UUID,
    val profilePublicKey: PlayerPublicKey,
) {

    companion object {
        fun validate(
            data: ChatSession,
            profileId: UUID,
            serviceSignatureValidator: ServiceSignatureValidator,
        ): RemoteChatSession {
            return RemoteChatSession(
                sessionId = data.sessionId(),
                profilePublicKey = data.publicKey().createValidated(
                    serviceSignatureValidator,
                    profileId,
                ),
            )
        }
    }

    fun createMessageDecoder(profileId: UUID): SignedMessageChain.Decoder {
        return SignedMessageChain(profileId, sessionId).decoder(profilePublicKey)
    }

    fun hasExpired(): Boolean = profilePublicKey.hasExpired()

    fun asData(): ChatSession = ChatSession(sessionId, profilePublicKey)
}
