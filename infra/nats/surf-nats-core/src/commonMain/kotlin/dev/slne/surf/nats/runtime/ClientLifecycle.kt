package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Open, closing and closed state of a client.
 *
 * While closing, new registrations are rejected but running handlers may still publish and call through the
 * client; once closed, every use is rejected. Thread-safe.
 */
@InternalNatsApi
@OptIn(ExperimentalAtomicApi::class)
internal class ClientLifecycle(name: String) {
    val label = "NatsClient[$name]"

    private val state = AtomicReference(State.OPEN)

    val isClosed: Boolean get() = state.load() == State.CLOSED

    fun requireOpen() = check(!isClosed) { "$label is closed" }

    fun requireAccepting() = check(state.load() == State.OPEN) {
        if (isClosed) "$label is closed" else "$label is closing"
    }

    /**
     * Moves from open to closing; returns `false` when closing already started.
     */
    fun beginClose(): Boolean = state.compareAndSet(State.OPEN, State.CLOSING)

    fun markClosed() = state.store(State.CLOSED)

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

    private enum class State { OPEN, CLOSING, CLOSED }
}
