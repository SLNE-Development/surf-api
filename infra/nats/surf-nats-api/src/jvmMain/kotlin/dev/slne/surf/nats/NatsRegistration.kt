package dev.slne.surf.nats

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Blocking

/**
 * Represents a listener or service registration that can be removed with [close].
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
     * effect.
     * Listener registrations remove their listeners immediately and unsubscribe when no listeners
     * remain, without waiting for running invocations. Service registrations stop accepting new
     * requests and allow queued requests and running handlers to complete within the configured
     * drain timeout (10 seconds by default), then cancel any remaining handlers. Closing a service
     * registration blocks the calling thread while it drains. Concurrent calls may return before
     * draining has finished.
     */
    @Blocking
    override fun close()
}
