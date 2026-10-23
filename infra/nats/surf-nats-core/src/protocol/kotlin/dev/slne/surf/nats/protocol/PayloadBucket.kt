package dev.slne.surf.nats.protocol

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal enum class PayloadBucket(val bucketName: String, val ttl: Duration) {
    TRANSIENT(ProtocolDefaults.TRANSIENT_PAYLOAD_BUCKET, ProtocolDefaults.TRANSIENT_PAYLOAD_TTL_SECONDS.seconds),
    PERSISTENT(ProtocolDefaults.PERSISTENT_PAYLOAD_BUCKET, ProtocolDefaults.PERSISTENT_PAYLOAD_TTL_SECONDS.seconds);

    fun lifetimeExceededMessage(bodyBytes: Int, lifetime: Duration): String {
        return "Payload of $bodyBytes bytes must be offloaded, but its lifetime $lifetime exceeds " +
                "the TTL of $ttl (bucket $bucketName)"
    }

    companion object {
        fun jetStreamDisabledMessage(maxPayload: Long): String {
            return "Payload exceeds the NATS max payload of $maxPayload bytes and " +
                    "cannot be offloaded: JetStream is not enabled on the server"
        }
    }
}
