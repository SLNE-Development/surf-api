package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.rpc.DeadlineLimiter.Admission
import dev.slne.surf.nats.rpc.DeadlineLimiter.Lane
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

// Real time: the limiter compares deadlines with the wall clock
class DeadlineLimiterTest {

    @Test
    fun `free permits are granted without waiting`() = runBlocking {
        val limiter = DeadlineLimiter(2)

        assertGranted(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT))
        assertGranted(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT))
        assertEquals(
            Admission.EXPIRED,
            limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT),
            "no permit left and no time to wait"
        )
    }

    @Test
    fun `waiters are admitted earliest deadline first`() = runBlocking {
        val limiter = DeadlineLimiter(1)
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))

        val admitted = mutableListOf<String>()
        val waiters = listOf("late" to 4.seconds, "earliest" to 2.seconds, "early" to 3.seconds).map { (name, left) ->
            launch(start = CoroutineStart.UNDISPATCHED) {
                assertGranted(limiter.acquire(deadlineIn(left), Lane.SHORT))
                admitted += name
                limiter.release(Lane.SHORT)
            }
        }

        limiter.release(Lane.SHORT)
        waiters.joinAll()
        assertEquals(listOf("earliest", "early", "late"), admitted)
    }

    @Test
    fun `waiters with the same deadline are admitted in arrival order`() = runBlocking {
        val limiter = DeadlineLimiter(1)
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))

        val deadline = deadlineIn(2.seconds)
        val admitted = mutableListOf<Int>()
        val waiters = (1..3).map { arrival ->
            launch(start = CoroutineStart.UNDISPATCHED) {
                assertGranted(limiter.acquire(deadline, Lane.SHORT))
                admitted += arrival
                limiter.release(Lane.SHORT)
            }
        }

        limiter.release(Lane.SHORT)
        waiters.joinAll()
        assertEquals(listOf(1, 2, 3), admitted)
    }

    @Test
    fun `waiter whose deadline passes gets no permit and takes none`() = runBlocking {
        val limiter = DeadlineLimiter(1)
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))

        assertEquals(Admission.EXPIRED, limiter.acquire(deadlineIn(100.milliseconds), Lane.SHORT))

        limiter.release(Lane.SHORT)
        assertIdle(limiter)
    }

    @Test
    fun `released permit skips a waiter whose deadline has passed`() = runBlocking {
        val limiter = DeadlineLimiter(1)
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))
        val expired = async(start = CoroutineStart.UNDISPATCHED) {
            limiter.acquire(deadlineIn(50.milliseconds), Lane.SHORT)
        }
        val next = async(start = CoroutineStart.UNDISPATCHED) { limiter.acquire(deadlineIn(5.seconds), Lane.SHORT) }

        // Blocks the only thread, so the timeout of the expired waiter cannot run before the release
        Thread.sleep(100)
        limiter.release(Lane.SHORT)

        assertEquals(Admission.EXPIRED, expired.await())
        assertGranted(withTimeout(1.seconds) { next.await() })
        limiter.release(Lane.SHORT)
        assertIdle(limiter)
    }

    @Test
    fun `cancelled waiter passes the permit on`() = runBlocking {
        val limiter = DeadlineLimiter(1)
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))

        val cancelled = launch(start = CoroutineStart.UNDISPATCHED) {
            limiter.acquire(deadlineIn(1.seconds), Lane.SHORT)
        }
        val next = async(start = CoroutineStart.UNDISPATCHED) { limiter.acquire(deadlineIn(2.seconds), Lane.SHORT) }

        cancelled.cancelAndJoin()
        limiter.release(Lane.SHORT)
        assertGranted(withTimeout(1.seconds) { next.await() })
    }

    @Test
    fun `waiter cancelled after it was handed a permit releases it once`() = runBlocking {
        val limiter = DeadlineLimiter(permits = 2, longPermits = 1)
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.LONG))
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))
        val handedOver = launch(start = CoroutineStart.UNDISPATCHED) {
            if (limiter.acquire(deadlineIn(5.seconds), Lane.LONG) == Admission.GRANTED) limiter.release(Lane.LONG)
        }

        // The release decides the handover on this thread; the waiter is cancelled before it is dispatched
        limiter.release(Lane.LONG)
        handedOver.cancelAndJoin()

        limiter.release(Lane.SHORT)
        assertIdle(limiter)
    }

    @Test
    fun `permit handed over while its waiter times out stays granted`() = runBlocking {
        val limiter = DeadlineLimiter(1)
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))
        val held = HeldDispatcher()
        val waiter = CoroutineScope(held).async { limiter.acquire(deadlineIn(200.milliseconds), Lane.SHORT) }
        held.runPending()
        assertEquals(1, limiter.waiterCount)

        // The handover is decided before the deadline, but the waiter resumes only after its timeout has fired
        limiter.release(Lane.SHORT)
        Thread.sleep(400)
        held.runPending()

        assertGranted(waiter.await())
        limiter.release(Lane.SHORT)
        assertIdle(limiter)
    }

    @Test
    fun `closing rejects waiters and later arrivals but grants free permits`() = runBlocking {
        val limiter = DeadlineLimiter(2)
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))
        val waiter = async(start = CoroutineStart.UNDISPATCHED) { limiter.acquire(deadlineIn(5.seconds), Lane.SHORT) }

        limiter.close()

        assertEquals(Admission.CLOSED, withTimeout(1.seconds) { waiter.await() })
        assertEquals(Admission.CLOSED, limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))
        limiter.release(Lane.SHORT)
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))
        repeat(2) { limiter.release(Lane.SHORT) }
        assertIdle(limiter)
    }

    @Test
    fun `long handlers take at most their share and short ones keep the rest`() = runBlocking {
        val limiter = DeadlineLimiter(permits = 4, longPermits = 2)

        assertGranted(limiter.acquire(deadlineIn(Duration.ZERO), Lane.LONG))
        assertGranted(limiter.acquire(deadlineIn(Duration.ZERO), Lane.LONG))
        assertEquals(Admission.EXPIRED, limiter.acquire(deadlineIn(Duration.ZERO), Lane.LONG), "long share exhausted")
        assertGranted(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT))
        assertGranted(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT))
        assertEquals(Admission.EXPIRED, limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT), "all permits taken")
    }

    @Test
    fun `released short permit skips a long waiter over its share`() = runBlocking {
        val limiter = DeadlineLimiter(permits = 2, longPermits = 1)
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.LONG))
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))

        val long = async(start = CoroutineStart.UNDISPATCHED) { limiter.acquire(deadlineIn(1.seconds), Lane.LONG) }
        val short = async(start = CoroutineStart.UNDISPATCHED) { limiter.acquire(deadlineIn(3.seconds), Lane.SHORT) }

        limiter.release(Lane.SHORT)
        assertGranted(withTimeout(1.seconds) { short.await() }, "the short waiter gets the freed permit")
        assertFalse(long.isCompleted, "the long waiter must wait for a long permit")

        limiter.release(Lane.LONG)
        assertGranted(withTimeout(1.seconds) { long.await() })
    }

    @Test
    fun `cancelled long waiter takes no long share`() = runBlocking {
        val limiter = DeadlineLimiter(permits = 2, longPermits = 1)
        assertGranted(limiter.acquire(deadlineIn(5.seconds), Lane.LONG))

        val cancelled = launch(start = CoroutineStart.UNDISPATCHED) { limiter.acquire(deadlineIn(2.seconds), Lane.LONG) }
        cancelled.cancelAndJoin()
        limiter.release(Lane.LONG)

        assertIdle(limiter)
    }

    @Test
    fun `permits stay balanced under concurrent use`() = runBlocking(Dispatchers.Default) {
        val limiter = DeadlineLimiter(permits = 4, longPermits = 2)
        val running = AtomicInteger()
        val runningLong = AtomicInteger()
        val peak = AtomicInteger()
        val peakLong = AtomicInteger()

        coroutineScope {
            repeat(400) { index ->
                val lane = if (index % 3 == 0) Lane.LONG else Lane.SHORT
                launch {
                    if (limiter.acquire(deadlineIn(10.seconds), lane) != Admission.GRANTED) return@launch
                    peak.accumulateAndGet(running.incrementAndGet(), ::maxOf)
                    if (lane == Lane.LONG) peakLong.accumulateAndGet(runningLong.incrementAndGet(), ::maxOf)
                    delay(1.milliseconds)
                    if (lane == Lane.LONG) runningLong.decrementAndGet()
                    running.decrementAndGet()
                    limiter.release(lane)
                }
            }
        }

        assertTrue(peak.get() <= 4, "peak ${peak.get()} exceeds the limit")
        assertTrue(peakLong.get() <= 2, "long peak ${peakLong.get()} exceeds the long share")
        assertIdle(limiter)
    }

    @Test
    fun `permits handed over around the deadlines of their waiters are not lost`() = runBlocking(Dispatchers.Default) {
        val limiters = List(4) { DeadlineLimiter(permits = 2, longPermits = 1) }

        limiters.map { limiter -> launch { handOverAroundDeadlines(limiter) } }.joinAll()

        limiters.forEach(::assertIdle)
    }

    @Test
    fun `permits handed over to waiters cancelled at the same time are not lost`() = runBlocking(Dispatchers.Default) {
        val limiter = DeadlineLimiter(permits = 2, longPermits = 1)

        repeat(1_000) {
            holdAllPermits(limiter)
            val waiters = List(6) { index ->
                val lane = LANES[index % 2]
                launch { if (limiter.acquire(deadlineIn(5.seconds), lane) == Admission.GRANTED) limiter.release(lane) }
            }
            while (limiter.waiterCount < waiters.size) yield()
            LANES.forEach(limiter::release)
            waiters.forEach { it.cancel() }
            waiters.joinAll()
        }

        assertIdle(limiter)
    }

    // Holds every permit, starts waiters whose deadlines are a few ms away and releases right around them
    private suspend fun handOverAroundDeadlines(limiter: DeadlineLimiter) = coroutineScope {
        repeat(1_000) { round ->
            holdAllPermits(limiter)
            val waiters = List(6) { index ->
                val lane = LANES[index % 2]
                launch {
                    val deadline = deadlineIn((1 + index % 3).milliseconds)
                    if (limiter.acquire(deadline, lane) == Admission.GRANTED) limiter.release(lane)
                }
            }
            val releaseAt = System.nanoTime() + 500_000L + (round % 10) * 250_000L
            while (System.nanoTime() < releaseAt) Thread.onSpinWait()
            LANES.forEach(limiter::release)
            waiters.joinAll()
        }
    }

    private suspend fun holdAllPermits(limiter: DeadlineLimiter) {
        for (lane in LANES) {
            check(limiter.acquire(deadlineIn(Duration.ZERO), lane) == Admission.GRANTED) { "a permit was lost" }
        }
    }

    private fun assertIdle(limiter: DeadlineLimiter) {
        assertEquals(limiter.permits, limiter.availablePermits, "permits were lost or released twice")
        assertEquals(0, limiter.longPermitsInUse, "long permits were lost or released twice")
        assertEquals(0, limiter.waiterCount, "waiters were left behind")
    }

    private fun assertGranted(admission: Admission, message: String? = null) {
        assertEquals(Admission.GRANTED, admission, message)
    }

    private fun deadlineIn(left: Duration): Long = System.currentTimeMillis() + left.inWholeMilliseconds

    /**
     * Holds dispatched coroutines until [runPending], so a test decides when a resumed waiter runs.
     */
    private class HeldDispatcher : CoroutineDispatcher() {
        private val pending = ConcurrentLinkedQueue<Runnable>()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            pending += block
        }

        fun runPending() {
            while (true) pending.poll()?.run() ?: return
        }
    }

    private companion object {
        val LANES = listOf(Lane.LONG, Lane.SHORT)
    }
}
