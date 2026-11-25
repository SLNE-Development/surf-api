package dev.slne.surf.api.minestom.server.chat.impl.signature

import net.minestom.server.crypto.MessageSignature

internal data class LastSeenTrackedEntry(
    val signature: MessageSignature,
    val isPending: Boolean,
) {
    fun acknowledge(): LastSeenTrackedEntry {
        return if (isPending) copy(isPending = false) else this
    }
}
