package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.config.HandlerLimits
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.TreeSet
import kotlin.time.Duration.Companion.milliseconds

/**
 * Limits how many handlers run at the same time and admits waiting handlers earliest deadline first.
 *
 * Handlers in [Lane.LONG] may hold at most [longPermits] of the [permits], so long-running work cannot take every
 * permit away from short calls. Waiters with the same deadline are admitted in arrival order. A waiter whose deadline
 * has passed is never handed a permit. Every [Admission.GRANTED] must be paired with exactly one [release] of the
 * same lane. Thread-safe.
 *
 * Whether a waiter gets a permit is decided once, under the lock: by a [release] that hands it one, or by the waiter
 * itself when it stops waiting. A permit handed over while the waiter times out still counts as granted, and one
 * handed over while it is cancelled is released again, so no permit is lost or released twice.
 */
@InternalNatsApi
internal class DeadlineLimiter(val permits: Int, val longPermits: Int = HandlerLimits.defaultLong(permits)) {

    /**
     * Class of a handler. [LONG] handlers share at most [DeadlineLimiter.longPermits] permits, [SHORT] handlers may
     * take any permit.
     *
     * The service registration assigns [LONG] to fire calls, fan-outs and methods whose declared timeout exceeds the
     * core default timeout, and [SHORT] to all others.
     */
    enum class Lane { SHORT, LONG }

    /**
     * Result of [acquire].
     */
    enum class Admission {
        /**
         * A permit was granted and must be released.
         */
        GRANTED,

        /**
         * The deadline passed before a permit was free.
         */
        EXPIRED,

        /**
         * No permit was free and the limiter was closed, before or while waiting.
         */
        CLOSED,
    }

    init {
        require(permits > 0) { "Handler limit must be positive, was $permits" }
        require(longPermits in 1..permits) { "Long handler limit must be in 1..$permits, was $longPermits" }
    }

    private val lock = Any()
    private var available = permits
    private var longInUse = 0
    private var arrivals = 0L
    private var closed = false
    private val shortWaiters = TreeSet(WAITER_ORDER)
    private val longWaiters = TreeSet(WAITER_ORDER)

    /**
     * Number of permits not granted, for tests.
     */
    internal val availablePermits: Int get() = synchronized(lock) { available }

    /**
     * Number of granted [Lane.LONG] permits, for tests.
     */
    internal val longPermitsInUse: Int get() = synchronized(lock) { longInUse }

    /**
     * Number of waiters, for tests.
     */
    internal val waiterCount: Int get() = synchronized(lock) { shortWaiters.size + longWaiters.size }

    /**
     * Waits for a permit of [lane] until [deadlineMillis] (epoch milliseconds).
     *
     * A permit is granted without waiting when one is free, even if [deadlineMillis] has passed or the limiter is
     * closed. The caller checks its remaining time afterwards. Cancellation while waiting grants no permit.
     */
    suspend fun acquire(deadlineMillis: Long, lane: Lane): Admission {
        val waiter = synchronized(lock) {
            when {
                tryTake(lane) -> return Admission.GRANTED
                closed -> return Admission.CLOSED
                deadlineMillis <= System.currentTimeMillis() -> return Admission.EXPIRED
                else -> Waiter(deadlineMillis, arrivals++, lane).also { waitersOf(lane).add(it) }
            }
        }

        try {
            withTimeoutOrNull((deadlineMillis - System.currentTimeMillis()).milliseconds) { waiter.decided.await() }
        } catch (e: Throwable) {
            if (synchronized(lock) { settle(waiter) } == Admission.GRANTED) release(lane)
            throw e
        }
        return synchronized(lock) { settle(waiter) }
    }

    /**
     * Returns a permit of [lane] and hands one to the waiter with the earliest admissible deadline, if any. Waiters
     * whose deadline has passed are removed with [Admission.EXPIRED] on the way.
     */
    fun release(lane: Lane) {
        val decided = ArrayList<Waiter>(1)
        synchronized(lock) {
            available++
            if (lane == Lane.LONG) longInUse--
            handOver(decided)
        }
        decided.forEach(Waiter::wake)
    }

    /**
     * Stops queueing: current waiters get [Admission.CLOSED], and later calls of [acquire] get it instead of
     * waiting when no permit is free. Granted permits stay valid and are released as usual.
     */
    fun close() {
        val decided = synchronized(lock) {
            closed = true
            val waiters = shortWaiters + longWaiters
            shortWaiters.clear()
            longWaiters.clear()
            waiters.onEach { it.admission = Admission.CLOSED }
        }
        decided.forEach(Waiter::wake)
    }

    // Callers hold lock. Returns the decision for a waiter that stops waiting, deciding EXPIRED if none was made
    private fun settle(waiter: Waiter): Admission {
        waiter.admission?.let { return it }
        waitersOf(waiter.lane).remove(waiter)
        return Admission.EXPIRED.also { waiter.admission = it }
    }

    // Callers hold lock. Decides expired heads and the next admissible waiter into decided
    private fun handOver(decided: MutableList<Waiter>) {
        while (true) {
            val next = nextAdmissible() ?: return
            waitersOf(next.lane).remove(next)
            decided += next
            if (next.deadlineMillis <= System.currentTimeMillis()) {
                next.admission = Admission.EXPIRED
            } else {
                take(next.lane)
                next.admission = Admission.GRANTED
                return
            }
        }
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

    // Callers hold lock. Returns the earliest waiter that may take a permit now without removing it
    private fun nextAdmissible(): Waiter? {
        val short = shortWaiters.firstOrNull(Lane.SHORT)
        val long = longWaiters.firstOrNull(Lane.LONG)
        return when {
            short == null -> long
            long == null -> short
            else -> if (WAITER_ORDER.compare(short, long) <= 0) short else long
        }
    }

    private fun TreeSet<Waiter>.firstOrNull(lane: Lane): Waiter? =
        if (isEmpty() || !isAdmissible(lane)) null else first()

    private class Waiter(
        @JvmField val deadlineMillis: Long,
        @JvmField val arrival: Long,
        @JvmField val lane: Lane,
    ) {
        // Guarded by the lock of the limiter, null while waiting
        @JvmField
        var admission: Admission? = null

        @JvmField
        val decided = CompletableDeferred<Unit>()

        fun wake() {
            decided.complete(Unit)
        }
    }

    private companion object {
        val WAITER_ORDER = Comparator<Waiter> { a, b ->
            val byDeadline = a.deadlineMillis.compareTo(b.deadlineMillis)
            if (byDeadline != 0) byDeadline else a.arrival.compareTo(b.arrival)
        }
    }
}
