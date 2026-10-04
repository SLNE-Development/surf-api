package dev.slne.surf.api.minestom.server.chat.impl.signature

import net.kyori.adventure.chat.SignedMessage
import net.minestom.server.crypto.MessageSignature
import java.util.Base64


internal fun MessageSignature.checksum(): Int = signature().contentHashCode()

internal fun MessageSignature.adventure(): SignedMessage.Signature = SignedMessage.signature(signature())

internal fun MessageSignature.verify(validator: SignatureValidator, updater: SignatureUpdater): Boolean =
    validator.validate(updater, signature())


internal fun MessageSignature.pack(cache: MessageSignatureCache): MessageSignature.Packed {
    val packedId = cache.pack(this)
    return if (packedId != MessageSignatureCache.NOT_FOUND) {
        MessageSignature.Packed(packedId, null)
    } else {
        MessageSignature.Packed(this)
    }
}

internal fun MessageSignature?.describe(): String =
    if (this == null) "<no signature>" else Base64.getEncoder().encodeToString(signature())
