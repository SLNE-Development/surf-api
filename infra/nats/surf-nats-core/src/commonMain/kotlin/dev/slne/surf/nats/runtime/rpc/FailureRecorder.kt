package dev.slne.surf.nats.runtime.rpc

import dev.slne.surf.nats.InternalNatsApi

/**
 * Records the failures of an `ALL_INSTANCES` call: the first one is thrown, later ones are suppressed in it.
 *
 * Not thread-safe; one call records from one coroutine.
 */
@InternalNatsApi
internal open class FailureRecorder {
    private var failure: Throwable? = null

    fun record(error: Throwable) {
        val first = failure
        if (first == null) failure = error else first.addSuppressed(error)
    }

    fun throwIfAny() {
        failure?.let { throw it }
    }
}
