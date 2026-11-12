package dev.slne.surf.nats

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Blocking

/**
 * Represents a listener or service registration that can be removed with [close].
 *
 * Returned by [NatsClient.registerListener] and [NatsClient.registerService]. Closing the client closes all of its
 * registrations.
 */
@ApiStatus.NonExtendable
public interface NatsRegistration: AutoCloseable {
    /**
     * Whether closing this registration has begun.
     */
    public val isClosed: Boolean

    /**
     * Removes this registration.
     *
     * This operation is idempotent and thread-safe. Repeated or concurrent calls have no additional
     * effect, and may return before the first call has finished draining.
     *
     * A listener registration removes its listeners immediately and unsubscribes when no listeners
     * remain, without waiting for running invocations.
     *
     * A service registration stops accepting new requests and blocks the calling thread while running
     * handlers complete. Handlers still running after the configured drain timeout (10 seconds by default)
     * are cancelled. The call can block for up to about twice the drain timeout.
     *
     * Requests that wait for a free handler slot do not run. Core requests are dropped without a reply. Persistent
     * messages are released for redelivery, possibly to another instance, when deliveries and time are left.
     * Otherwise a fire is dead-lettered on its last delivery, and a persistent RPC is terminated on its last
     * delivery or when its caller would give up first.
     */
    @Blocking
    override fun close()
}
