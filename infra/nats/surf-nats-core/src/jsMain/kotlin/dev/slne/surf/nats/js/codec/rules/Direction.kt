package dev.slne.surf.nats.js.codec.rules

/**
 * Outgoing messages are only validated; incoming messages also get unknown enum numbers normalized.
 */
internal enum class Direction {
    OUTGOING,
    INCOMING,
}
