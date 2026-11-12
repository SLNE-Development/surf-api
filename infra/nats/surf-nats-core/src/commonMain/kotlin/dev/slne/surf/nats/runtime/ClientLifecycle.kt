package dev.slne.surf.nats.runtime

import dev.slne.surf.nats.InternalNatsApi
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Open, closing and closed state of a client.
 *
 * While closing, new registrations are rejected but running handlers may still publish and call through the
 * client. Once closed, every use is rejected. A client can stop accepting registrations ([stopAccepting]) before its
 * close starts ([beginClose]). Until then, outgoing work such as new service proxies stays allowed. Thread-safe.
 */
@InternalNatsApi
@OptIn(ExperimentalAtomicApi::class)
internal class ClientLifecycle(name: String) {
    val label = "NatsClient[$name]"

    private val state = AtomicReference(State.OPEN)

    private val closeStarted = AtomicBoolean(false)

    val isClosed: Boolean get() = state.load() == State.CLOSED

    /**
     * Throws [IllegalStateException] when the client is closed.
     */
    fun requireOpen() = check(!isClosed) { "$label is closed" }

    /**
     * Throws [IllegalStateException] when the client is closing or closed.
     */
    fun requireAccepting() = check(state.load() == State.OPEN) { rejection() }

    /**
     * Throws [IllegalStateException] when [beginClose] already ran.
     */
    fun requireCloseNotStarted() = check(!closeStarted.load()) { rejection() }

    /**
     * Moves from open to closing without starting the close.
     *
     * Returns `false` when new work was already rejected.
     */
    fun stopAccepting(): Boolean = state.compareAndSet(State.OPEN, State.CLOSING)

    /**
     * Starts the close and stops accepting new work.
     *
     * Returns `false` when the close already started.
     */
    fun beginClose(): Boolean {
        if (!closeStarted.compareAndSet(expectedValue = false, newValue = true)) return false
        stopAccepting()
        return true
    }

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

    /**
     * Runs [add] and keeps its effect only while the close has not started: when [add] fails or [beginClose] ran
     * meanwhile, [undo] reverts it and the failure is rethrown.
     */
    inline fun admitBeforeClose(add: () -> Unit, undo: () -> Unit) {
        try {
            add()
            requireCloseNotStarted()
        } catch (e: Throwable) {
            undo()
            throw e
        }
    }

    private fun rejection() = if (isClosed) "$label is closed" else "$label is closing"

    private enum class State { OPEN, CLOSING, CLOSED }
}
