package dev.slne.surf.api.minestom.chat.signature

import net.kyori.adventure.chat.SignedMessage
import net.minestom.server.crypto.MessageSignature
import java.util.Base64


fun MessageSignature.checksum(): Int = signature().contentHashCode()

fun MessageSignature.adventure(): SignedMessage.Signature = SignedMessage.signature(signature())

fun MessageSignature.verify(validator: SignatureValidator, updater: SignatureUpdater): Boolean =
    validator.validate(updater, signature())


fun MessageSignature.pack(cache: MessageSignatureCache): MessageSignature.Packed {
    val packedId = cache.pack(this)
    return if (packedId != MessageSignatureCache.NOT_FOUND) {
        MessageSignature.Packed(packedId, null)
    } else {
        MessageSignature.Packed(this)
    }
}

fun MessageSignature?.describe(): String =
    if (this == null) "<no signature>" else Base64.getEncoder().encodeToString(signature())
