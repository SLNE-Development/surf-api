package dev.slne.surf.nats.runtime.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolDefaults
import kotlin.time.Duration

/**
 * Wire contract of a service call per flow.
 *
 * [bucket] receives offloaded request bodies. [sendsDeadline], [sendsReplyTo] and [sendsTtl] tell whether the request
 * carries `Surf-Deadline`, `Surf-Reply-To` and `Nats-TTL`.
 */
@InternalNatsApi
internal enum class CallFlow(
    val bucket: PayloadBucket,
    val sendsDeadline: Boolean,
    val sendsReplyTo: Boolean,
    val sendsTtl: Boolean,
) {
    /**
     * Non-persistent call over core NATS request-reply.
     */
    CORE(PayloadBucket.TRANSIENT, sendsDeadline = true, sendsReplyTo = false, sendsTtl = false),

    /**
     * Persistent call without a result. The caller waits only for the stream acknowledgement, and the request may be
     * delivered until the stream max age.
     */
    FIRE(PayloadBucket.PERSISTENT, sendsDeadline = false, sendsReplyTo = false, sendsTtl = false),

    /**
     * Persistent call with a result. The responder replies to the `Surf-Reply-To` subject of the request.
     */
    PERSISTENT_RPC(PayloadBucket.TRANSIENT, sendsDeadline = true, sendsReplyTo = true, sendsTtl = true);

    /**
     * Returns how long an offloaded request body must stay readable.
     *
     * [FIRE] needs the stream max age because a fire may be delivered until then. Every other flow needs [timeout].
     */
    fun offloadLifetime(timeout: Duration): Duration = if (this == FIRE) ProtocolDefaults.PERSIST_MAX_AGE else timeout

    companion object {
        /**
         * Returns the flow of a method with the given persistence and result kind.
         */
        fun of(persistent: Boolean, resultKind: NatsResultKind): CallFlow = when {
            !persistent -> CORE
            resultKind == NatsResultKind.UNIT -> FIRE
            else -> PERSISTENT_RPC
        }
    }
}
