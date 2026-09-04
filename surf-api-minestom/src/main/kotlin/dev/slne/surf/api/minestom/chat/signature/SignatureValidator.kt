package dev.slne.surf.api.minestom.chat.signature

import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.security.PublicKey
import java.security.Signature

fun interface SignatureValidator {

    fun validate(updater: SignatureUpdater, signature: ByteArray): Boolean

    fun validate(payload: ByteArray, signature: ByteArray): Boolean =
        validate({ output -> output.update(payload) }, signature)

    companion object {
        private val LOGGER = ComponentLogger.logger()

        val NO_VALIDATION = SignatureValidator { _, _ -> true }

        fun from(publicKey: PublicKey, algorithm: String) =
            SignatureValidator { updater, signature ->
                try {
                    val verifier = Signature.getInstance(algorithm)
                    verifier.initVerify(publicKey)
                    updater.update(verifier::update)
                    verifier.verify(signature)
                } catch (e: Exception) {
                    LOGGER.error("Failed to verify signature", e)
                    false
                }
            }
    }
}
