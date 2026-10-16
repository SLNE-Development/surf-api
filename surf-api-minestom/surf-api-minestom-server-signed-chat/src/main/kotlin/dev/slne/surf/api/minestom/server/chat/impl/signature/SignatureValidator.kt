package dev.slne.surf.api.minestom.server.chat.impl.signature

import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.security.PublicKey
import java.security.Signature

internal fun interface SignatureValidator {

    fun validate(updater: SignatureUpdater, signature: ByteArray): Boolean

    companion object {
        private val LOGGER = ComponentLogger.logger()

        /**
         * A validator that verifies signatures with [algorithm] against [publicKey].
         *
         * A signature that cannot be verified, e.g. because it is malformed, is rejected. The
         * failure is logged at debug level only, as the signature is client input.
         */
        fun from(publicKey: PublicKey, algorithm: String) =
            SignatureValidator { updater, signature ->
                try {
                    val verifier = Signature.getInstance(algorithm)
                    verifier.initVerify(publicKey)
                    updater.update(verifier::update)
                    verifier.verify(signature)
                } catch (e: Exception) {
                    LOGGER.debug("Failed to verify signature: {}", e.toString())
                    false
                }
            }
    }
}
