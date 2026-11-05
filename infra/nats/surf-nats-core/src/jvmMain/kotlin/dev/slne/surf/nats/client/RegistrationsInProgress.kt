package dev.slne.surf.nats.client

import dev.slne.surf.nats.InternalNatsApi
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

/**
 * Counts the registrations of a client that are still subscribing, so stopping the client can wait for them.
 *
 * [enter] runs [requireAccepting] and counts the registration under one lock. A registration that passed the check
 * before the client stopped accepting is therefore counted before [awaitNone] reads the count. Thread-safe.
 */
@InternalNatsApi
internal class RegistrationsInProgress(private val requireAccepting: () -> Unit) {
    private val lock = ReentrantLock()
    private val none = lock.newCondition()
    private var count = 0

    /**
     * Counts a registration; throws the failure of [requireAccepting] without counting it. Pair with [exit].
     */
    fun enter() = lock.withLock {
        requireAccepting()
        count++
    }

    fun exit() = lock.withLock {
        if (--count == 0) none.signalAll()
    }

    /**
     * Waits until no registration is in progress; returns `false` when [timeout] passes first or the thread is
     * interrupted.
     */
    fun awaitNone(timeout: Duration): Boolean = lock.withLock {
        var nanos = timeout.inWholeNanoseconds
        while (count > 0) {
            if (nanos <= 0) return false
            nanos = try {
                none.awaitNanos(nanos)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        true
    }
}
