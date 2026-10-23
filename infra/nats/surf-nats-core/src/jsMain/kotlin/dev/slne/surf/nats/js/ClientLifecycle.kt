package dev.slne.surf.nats.js

/**
 * Open, closing and closed state of a [NatsRuntime], like the JVM `ClientLifecycle` (single-threaded here).
 *
 * While closing, new listeners are rejected but running handlers may still publish and call through the client; once
 * closed, every use is rejected.
 */
internal class ClientLifecycle(private val name: String) {
    private var state = State.OPEN

    val isClosed: Boolean get() = state == State.CLOSED

    fun requireOpen() = check(state != State.CLOSED) { "SurfNatsClient[$name] is closed" }

    fun requireAccepting() = check(state == State.OPEN) {
        if (isClosed) "SurfNatsClient[$name] is closed" else "SurfNatsClient[$name] is closing"
    }

    /**
     * Moves from open to closing; returns `false` when closing already started.
     */
    fun beginClose(): Boolean {
        if (state != State.OPEN) return false
        state = State.CLOSING
        return true
    }

    fun markClosed() {
        state = State.CLOSED
    }

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
