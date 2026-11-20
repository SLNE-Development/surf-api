package dev.slne.surf.api.spring.util

import dev.slne.surf.api.shared.api.util.InternalSurfApi

/**
 * Collects the failures of steps that all have to run, even when an earlier one fails.
 *
 * The first recorded failure is kept as [first]; every later one is added to it as a suppressed
 * exception. The caller decides which exceptions count as a step failure and catches them itself.
 * Not thread-safe.
 */
@InternalSurfApi
class FailureCollector {

    /**
     * The first recorded failure, or `null` while every step succeeded.
     */
    var first: Throwable? = null
        private set

    /**
     * Records [failure]: as [first] if it is the first one, otherwise as suppressed by [first].
     */
    fun add(failure: Throwable) {
        val current = first
        if (current == null) first = failure else current.addSuppressed(failure)
    }

    /**
     * Throws [first], if any step failed.
     */
    fun throwIfAny() {
        first?.let { throw it }
    }
}
