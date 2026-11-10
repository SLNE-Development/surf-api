package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Limits how many handlers run at the same time and admits waiting handlers earliest deadline first.
 *
 * Handlers in [Lane.LONG] may hold at most [longPermits] of the [permits], so long-running work cannot take every
 * permit away from short calls. Waiters with the same deadline are admitted in arrival order; a waiter whose deadline
 * passes is not admitted. Every successful [acquire] must be paired with exactly one [release] of the same lane.
 * Thread-safe.
 */
@InternalNatsApi
internal class DeadlineLimiter(val permits: Int, val longPermits: Int = (permits / 2).coerceAtLeast(1)) {

    /**
     * Class of a handler: [SHORT] for calls whose caller waits at most the core default timeout, [LONG] for all others.
     */
    enum class Lane { SHORT, LONG }

    init {
        require(permits > 0) { "Handler limit must be positive, was $permits" }
        require(longPermits in 1..permits) { "Long handler limit must be in 1..$permits, was $longPermits" }
    }

    private val lock = Any()
    private var available = permits
    private var longInUse = 0
    private var arrivals = 0L
    private val shortWaiters = ObjectRBTreeSet(WAITER_ORDER)
    private val longWaiters = ObjectRBTreeSet(WAITER_ORDER)

    /**
     * Waits for a permit of [lane] until [deadlineMillis] (epoch milliseconds) and returns whether one was granted.
     *
     * A permit is granted without waiting when one is free, even if [deadlineMillis] has passed; the caller checks
     * its remaining time afterwards. Cancellation while waiting grants no permit.
     */
    suspend fun acquire(deadlineMillis: Long, lane: Lane): Boolean {
        if (synchronized(lock) { tryTake(lane) }) return true

        val wait = (deadlineMillis - System.currentTimeMillis()).milliseconds
        if (!wait.isPositive()) return false

        return withTimeoutOrNull(wait) {
            suspendCancellableCoroutine { continuation ->
                enqueue(continuation, deadlineMillis, lane)
            }
        } != null
    }

    /**
     * Returns a permit of [lane] and hands one to the waiter with the earliest admissible deadline, if any.
     */
    fun release(lane: Lane) {
        val next = synchronized(lock) {
            available++
            if (lane == Lane.LONG) longInUse--
            nextAdmissible()?.also { take(it.lane) }
        }
        // A waiter cancelled before it resumes passes the permit on
        next?.continuation?.resume(Unit) { _, _, _ -> release(next.lane) }
    }

    private fun enqueue(continuation: CancellableContinuation<Unit>, deadlineMillis: Long, lane: Lane) {
        val waiter = synchronized(lock) {
            if (tryTake(lane)) null else Waiter(deadlineMillis, arrivals++, lane, continuation).also(::addWaiter)
        }

        if (waiter == null) {
            continuation.resume(Unit) { _, _, _ -> release(lane) }
        } else {
            continuation.invokeOnCancellation { synchronized(lock) { waitersOf(waiter.lane).remove(waiter) } }
        }
    }

    private fun addWaiter(waiter: Waiter) {
        waitersOf(waiter.lane).add(waiter)
    }

    private fun waitersOf(lane: Lane) = if (lane == Lane.LONG) longWaiters else shortWaiters

    // Callers hold lock
    private fun tryTake(lane: Lane): Boolean {
        if (!isAdmissible(lane)) return false
        take(lane)
        return true
    }

    // Callers hold lock
    private fun take(lane: Lane) {
        available--
        if (lane == Lane.LONG) longInUse++
    }

    // Callers hold lock
    private fun isAdmissible(lane: Lane): Boolean =
        available > 0 && (lane == Lane.SHORT || longInUse < longPermits)

    // Callers hold lock; removes and returns the earliest waiter that may take a permit now
    private fun nextAdmissible(): Waiter? {
        val short = shortWaiters.firstOrNull(Lane.SHORT)
        val long = longWaiters.firstOrNull(Lane.LONG)
        val next = when {
            short == null -> long
            long == null -> short
            else -> if (WAITER_ORDER.compare(short, long) <= 0) short else long
        }
        next?.let { waitersOf(it.lane).remove(it) }
        return next
    }

    private fun ObjectRBTreeSet<Waiter>.firstOrNull(lane: Lane): Waiter? =
        if (isEmpty() || !isAdmissible(lane)) null else first()

    private class Waiter(
        @JvmField val deadlineMillis: Long,
        @JvmField val arrival: Long,
        @JvmField val lane: Lane,
        @JvmField val continuation: CancellableContinuation<Unit>,
    )

    private companion object {
        val WAITER_ORDER = Comparator<Waiter> { a, b ->
            val byDeadline = a.deadlineMillis.compareTo(b.deadlineMillis)
            if (byDeadline != 0) byDeadline else a.arrival.compareTo(b.arrival)
        }
    }
}
