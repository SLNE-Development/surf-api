package dev.slne.surf.nats.rpc

import java.io.Serial

/**
 * Requests that a persistent message is delivered again instead of being answered with a final error.
 *
 * Intended for transient infrastructure failures, such as an unavailable database, thrown by a
 * [NatsPersistentInterceptor] or a persistent handler. Business failures should throw other exceptions.
 *
 * - Persistent RPC: no reply is sent and the message is redelivered after the backoff of its delivery. On the last
 *   allowed delivery, or when the backoff would end after the caller's deadline, the caller receives this exception
 *   as an error reply instead.
 * - Persistent fire: handled like any other non-permanent failure, so the message is redelivered with backoff and
 *   dead-lettered after the last delivery.
 * - Core RPC: no redelivery exists; the caller receives this exception as an error reply.
 */
public class NatsRedeliveryException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    internal companion object {
        @Serial
        private const val serialVersionUID: Long = -3391805204375518112L
    }
}
