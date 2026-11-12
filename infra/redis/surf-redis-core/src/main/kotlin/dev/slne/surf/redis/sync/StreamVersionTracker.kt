package dev.slne.surf.redis.sync

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks the contiguous stream version a synchronized structure has applied locally.
 */
class StreamVersionTracker {
    private val lastVersion = AtomicLong(0L)
    private val bootstrapped = AtomicBoolean(false)

    val current: Long get() = lastVersion.get()
    val isBootstrapped: Boolean get() = bootstrapped.get()

    fun bootstrap(version: Long) {
        lastVersion.set(version)
        bootstrapped.set(true)
    }

    /**
     * Attempts to advance to [version].
     *
     * @return the outcome for the caller
     */
    fun apply(version: Long): Outcome {
        if (!bootstrapped.get()) return Outcome.RESYNC

        while (true) {
            val current = lastVersion.get()
            when {
                version <= current -> return Outcome.SKIPPED
                version != current + 1L -> return Outcome.RESYNC
                lastVersion.compareAndSet(current, version) -> return Outcome.APPLIED
            }
        }
    }

    /**
     * Attempts to advance across the contiguous range `[first, last]` produced by a batched local
     * mutation.
     */
    fun applyRange(first: Long, last: Long): Outcome {
        if (!bootstrapped.get()) return Outcome.RESYNC
        if (first !in 1..last) return Outcome.RESYNC

        while (true) {
            val current = lastVersion.get()
            when {
                last <= current -> return Outcome.SKIPPED
                first != current + 1L -> return Outcome.RESYNC
                lastVersion.compareAndSet(current, last) -> return Outcome.APPLIED
            }
        }
    }

    enum class Outcome {
        /** The caller advanced the version and owns the corresponding change. */
        APPLIED,

        /** The version was already covered; the caller should do nothing. */
        SKIPPED,

        /** A gap or an un-bootstrapped tracker was observed; the caller should resynchronize. */
        RESYNC
    }
}
