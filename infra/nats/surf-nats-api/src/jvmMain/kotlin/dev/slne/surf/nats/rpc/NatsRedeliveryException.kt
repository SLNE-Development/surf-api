package dev.slne.surf.nats.rpc

import java.io.Serial

/**
 * Requests that a persistent message is delivered again instead of being answered with a final error.
 *
 * Throw it from a persistent handler or a [NatsPersistentInterceptor] for transient infrastructure failures, such as
 * an unavailable database. Throw other exceptions for domain failures.
 *
 * - Persistent RPC: no reply is sent and the message is redelivered after the backoff of its delivery. On the last
 *   allowed delivery, or when the backoff would end after the caller's deadline, the caller receives this exception
 *   as a [NatsRemoteException] instead.
 * - Persistent fire: handled like any other failure that is not permanent, so the message is redelivered with
 *   backoff and dead-lettered after the last delivery.
 * - Methods that are not persistent: no redelivery exists, and the caller receives this exception as a
 *   [NatsRemoteException].
 *
 * @param message the detail message, which the caller receives when no redelivery happens.
 * @param cause the underlying failure, which is not sent to the caller.
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
