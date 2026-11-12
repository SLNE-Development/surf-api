package dev.slne.surf.nats.protocol

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * JetStream object store buckets that hold offloaded message bodies.
 *
 * An offloaded body is stored under a random object name that the message carries in `Surf-Payload-Ref`. Each bucket
 * is created with [ttl], so a body must be readable for at most that long. [TRANSIENT] (10 minutes) holds requests and
 * replies of calls a caller waits for. [PERSISTENT] (7 days, the max age of the persist streams) holds fire requests
 * that may be redelivered until the stream drops them.
 */
internal enum class PayloadBucket(val bucketName: String, val ttl: Duration) {
    TRANSIENT(ProtocolDefaults.TRANSIENT_PAYLOAD_BUCKET, ProtocolDefaults.TRANSIENT_PAYLOAD_TTL_SECONDS.seconds),
    PERSISTENT(ProtocolDefaults.PERSISTENT_PAYLOAD_BUCKET, ProtocolDefaults.PERSISTENT_PAYLOAD_TTL_SECONDS.seconds);

    /**
     * Returns the error message for a body that must be offloaded but has to outlive [ttl].
     */
    fun lifetimeExceededMessage(bodyBytes: Int, lifetime: Duration): String {
        return "Payload of $bodyBytes bytes must be offloaded, but its lifetime $lifetime exceeds " +
                "the TTL of $ttl (bucket $bucketName)"
    }

    companion object {
        /**
         * Returns the error message for a body above [maxPayload] on a server without JetStream.
         */
        fun jetStreamDisabledMessage(maxPayload: Long): String {
            return "Payload exceeds the NATS max payload of $maxPayload bytes and " +
                    "cannot be offloaded: JetStream is not enabled on the server"
        }
    }
}
