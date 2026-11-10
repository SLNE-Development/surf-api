package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.rpc.DeadlineLimiter.Lane
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger
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

        assertTrue(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT))
        assertTrue(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT))
        assertFalse(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT), "no permit left and no time to wait")
    }

    @Test
    fun `waiters are admitted earliest deadline first`() = runBlocking {
        val limiter = DeadlineLimiter(1)
        assertTrue(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))

        val admitted = mutableListOf<String>()
        val waiters = listOf("late" to 4.seconds, "earliest" to 2.seconds, "early" to 3.seconds).map { (name, left) ->
            launch(start = CoroutineStart.UNDISPATCHED) {
                assertTrue(limiter.acquire(deadlineIn(left), Lane.SHORT))
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
        assertTrue(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))

        val deadline = deadlineIn(2.seconds)
        val admitted = mutableListOf<Int>()
        val waiters = (1..3).map { arrival ->
            launch(start = CoroutineStart.UNDISPATCHED) {
                assertTrue(limiter.acquire(deadline, Lane.SHORT))
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
        assertTrue(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))

        assertFalse(limiter.acquire(deadlineIn(100.milliseconds), Lane.SHORT))

        limiter.release(Lane.SHORT)
        assertTrue(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT), "the expired waiter must not hold the permit")
    }

    @Test
    fun `cancelled waiter passes the permit on`() = runBlocking {
        val limiter = DeadlineLimiter(1)
        assertTrue(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))

        val cancelled = launch(start = CoroutineStart.UNDISPATCHED) {
            limiter.acquire(deadlineIn(1.seconds), Lane.SHORT)
        }
        val next = async(start = CoroutineStart.UNDISPATCHED) { limiter.acquire(deadlineIn(2.seconds), Lane.SHORT) }

        cancelled.cancelAndJoin()
        limiter.release(Lane.SHORT)
        assertTrue(withTimeout(1.seconds) { next.await() })
    }

    @Test
    fun `long handlers take at most their share and short ones keep the rest`() = runBlocking {
        val limiter = DeadlineLimiter(permits = 4, longPermits = 2)

        assertTrue(limiter.acquire(deadlineIn(Duration.ZERO), Lane.LONG))
        assertTrue(limiter.acquire(deadlineIn(Duration.ZERO), Lane.LONG))
        assertFalse(limiter.acquire(deadlineIn(Duration.ZERO), Lane.LONG), "long share exhausted")
        assertTrue(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT))
        assertTrue(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT))
        assertFalse(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT), "all permits taken")
    }

    @Test
    fun `released short permit skips a long waiter over its share`() = runBlocking {
        val limiter = DeadlineLimiter(permits = 2, longPermits = 1)
        assertTrue(limiter.acquire(deadlineIn(5.seconds), Lane.LONG))
        assertTrue(limiter.acquire(deadlineIn(5.seconds), Lane.SHORT))

        val long = async(start = CoroutineStart.UNDISPATCHED) { limiter.acquire(deadlineIn(1.seconds), Lane.LONG) }
        val short = async(start = CoroutineStart.UNDISPATCHED) { limiter.acquire(deadlineIn(3.seconds), Lane.SHORT) }

        limiter.release(Lane.SHORT)
        assertTrue(withTimeout(1.seconds) { short.await() }, "the short waiter gets the freed permit")
        assertFalse(long.isCompleted, "the long waiter must wait for a long permit")

        limiter.release(Lane.LONG)
        assertTrue(withTimeout(1.seconds) { long.await() })
    }

    @Test
    fun `cancelled long handoff returns the long share`() = runBlocking {
        val limiter = DeadlineLimiter(permits = 2, longPermits = 1)
        assertTrue(limiter.acquire(deadlineIn(5.seconds), Lane.LONG))

        val cancelled = launch(start = CoroutineStart.UNDISPATCHED) { limiter.acquire(deadlineIn(2.seconds), Lane.LONG) }
        cancelled.cancelAndJoin()
        limiter.release(Lane.LONG)

        assertTrue(limiter.acquire(deadlineIn(Duration.ZERO), Lane.LONG), "long share was not returned")
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
                    if (!limiter.acquire(deadlineIn(10.seconds), lane)) return@launch
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
        repeat(2) { assertTrue(limiter.acquire(deadlineIn(Duration.ZERO), Lane.LONG), "long permit $it was lost") }
        repeat(2) { assertTrue(limiter.acquire(deadlineIn(Duration.ZERO), Lane.SHORT), "permit $it was lost") }
    }

    private fun deadlineIn(left: Duration): Long = System.currentTimeMillis() + left.inWholeMilliseconds
}
