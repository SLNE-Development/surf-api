package dev.slne.surf.api.minestom.server.chat.impl.signature

import net.kyori.adventure.text.Component
import net.minestom.server.crypto.PlayerPublicKey
import net.minestom.server.utils.crypto.KeyUtils
import java.io.Serial
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.util.UUID
import net.minestom.server.crypto.SignatureValidator as ServiceSignatureValidator

internal val EXPIRED_PROFILE_PUBLIC_KEY: Component =
    Component.translatable("multiplayer.disconnect.expired_public_key")

internal val INVALID_PROFILE_PUBLIC_KEY_SIGNATURE: Component =
    Component.translatable("multiplayer.disconnect.invalid_public_key_signature")


internal class ProfilePublicKeyValidationException(val component: Component) : Exception(null, null, false, false) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -4832597595024741094L
    }
}

internal fun PlayerPublicKey.createValidated(
    validator: ServiceSignatureValidator,
    profileId: UUID
): PlayerPublicKey {
    if (!validateSignature(validator, profileId)) {
        throw ProfilePublicKeyValidationException(INVALID_PROFILE_PUBLIC_KEY_SIGNATURE)
    }

    return this
}

@Suppress("UnstableApiUsage")
internal fun PlayerPublicKey.createSignatureValidator(): SignatureValidator =
    SignatureValidator.from(publicKey(), KeyUtils.SignatureAlgorithm.SHA256withRSA.name)

internal fun PlayerPublicKey.hasExpired() = expiresAt().isBefore(Instant.now())

internal fun PlayerPublicKey.validateSignature(
    validator: ServiceSignatureValidator,
    profileId: UUID
): Boolean = validator.validate(signedPayload(profileId), signature())

private fun PlayerPublicKey.signedPayload(profileId: UUID): ByteArray {
    val keyBytes = publicKey().encoded
    val signedPayload = ByteArray(24 + keyBytes.size)

    ByteBuffer.wrap(signedPayload).order(ByteOrder.BIG_ENDIAN)
        .putLong(profileId.mostSignificantBits)
        .putLong(profileId.leastSignificantBits)
        .putLong(expiresAt().toEpochMilli())
        .put(keyBytes)

    return signedPayload
}
