package dev.slne.surf.redis.codec

import java.io.Serial

/**
 * Indicates that a user-provided Redis codec failed or produced an invalid payload.
 *
 * Messages include the codec identity and the event or synchronized-structure context in which
 * the failure occurred.
 */
class RedisCodecException @JvmOverloads constructor(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    companion object {
        @Serial
        private const val serialVersionUID: Long = -1076029492684152080L
    }
}
