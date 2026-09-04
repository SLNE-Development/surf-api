package dev.slne.surf.api.minestom.chat.signature

import java.security.PrivateKey
import java.security.Signature


fun interface Signer {
    fun sign(updater: SignatureUpdater): ByteArray

    fun sign(payload: ByteArray): ByteArray = sign { output -> output.update(payload) }

    companion object {
        fun from(privateKey: PrivateKey, algorithm: String) = Signer { updater ->
            try {
                val signer = Signature.getInstance(algorithm)
                signer.initSign(privateKey)
                updater.update(signer::update)
                signer.sign()
            } catch (e: Exception) {
                throw IllegalStateException("Failed to sign message", e)
            }
        }
    }
}
