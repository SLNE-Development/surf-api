package dev.slne.surf.nats.client

import dev.slne.surf.nats.InternalNatsApi
import java.util.concurrent.atomic.AtomicInteger

/**
 * Open, closing and closed state of a [NatsClientImpl].
 *
 * While closing, new registrations and proxies are rejected but running handlers may still publish and call through
 * the client; once closed, every use is rejected.
 */
@InternalNatsApi
internal class ClientLifecycle(private val name: String) {
    private val state = AtomicInteger(OPEN)

    val isClosed: Boolean get() = state.get() == CLOSED

    fun requireOpen() = check(state.get() != CLOSED) { "NatsClient[$name] is closed" }

    fun requireAccepting() = check(state.get() == OPEN) {
        if (isClosed) "NatsClient[$name] is closed" else "NatsClient[$name] is closing"
    }

    /**
     * Moves from open to closing; returns `false` when closing already started.
     */
    fun beginClose(): Boolean = state.compareAndSet(OPEN, CLOSING)

    fun markClosed() = state.set(CLOSED)

    /**
     * Runs [add] and keeps its effect only while new work is accepted: when [add] fails or closing started meanwhile,
     * [undo] reverts it and the failure is rethrown.
     */
    inline fun admit(add: () -> Unit, undo: () -> Unit) {
        try {
            add()
            requireAccepting()
        } catch (e: Throwable) {
            undo()
            throw e
        }
    }

    private companion object {
        const val OPEN = 0
        const val CLOSING = 1
        const val CLOSED = 2
    }
}
