package dev.slne.surf.api.minestom.server.chat.impl.signature

import net.minestom.server.crypto.MessageSignature


internal data class LastSeenTrackedEntry(
    val signature: MessageSignature,
    val pending: Boolean
) {
    fun acknowledge(): LastSeenTrackedEntry = if (!pending) this else copy(pending = false)
}
