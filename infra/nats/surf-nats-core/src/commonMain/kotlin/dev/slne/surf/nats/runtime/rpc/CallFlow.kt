package dev.slne.surf.nats.runtime.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import kotlin.time.Duration

/**
 * Wire contract of a service call per flow (spec 12.6).
 *
 * [bucket] receives offloaded request bodies; [sendsDeadline], [sendsReplyTo] and [sendsTtl] tell whether the request
 * carries `Surf-Deadline`, `Surf-Reply-To` and `Nats-TTL`.
 */
@InternalNatsApi
internal enum class CallFlow(
    val bucket: PayloadBucket,
    val sendsDeadline: Boolean,
    val sendsReplyTo: Boolean,
    val sendsTtl: Boolean,
) {
    CORE(PayloadBucket.TRANSIENT, sendsDeadline = true, sendsReplyTo = false, sendsTtl = false),
    FIRE(PayloadBucket.PERSISTENT, sendsDeadline = false, sendsReplyTo = false, sendsTtl = false),
    PERSISTENT_RPC(PayloadBucket.TRANSIENT, sendsDeadline = true, sendsReplyTo = true, sendsTtl = true);

    /**
     * Returns how long an offloaded request body must stay readable: a fire may be delivered until the stream max age.
     */
    fun offloadLifetime(timeout: Duration): Duration = if (this == FIRE) ProtocolDefaults.PERSIST_MAX_AGE else timeout

    companion object {
        fun of(persistent: Boolean, resultKind: NatsResultKind): CallFlow = when {
            !persistent -> CORE
            resultKind == NatsResultKind.UNIT -> FIRE
            else -> PERSISTENT_RPC
        }
    }
}
