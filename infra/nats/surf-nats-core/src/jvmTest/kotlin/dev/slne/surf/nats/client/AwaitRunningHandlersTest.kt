package dev.slne.surf.nats.client

import kotlinx.coroutines.*
import org.junit.jupiter.api.AfterEach
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue

// Real time: the wait blocks the calling thread until a monotonic deadline
class AwaitRunningHandlersTest {

    private val handlers = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> })

    // Runs the waiting call only: a helper launched into handlers would count as a handler
    private val background = CoroutineScope(Dispatchers.IO)

    @AfterEach
    fun cancelScopes() {
        handlers.cancel()
        background.cancel()
    }

    @Test
    fun `returns null without waiting when no handler is running`() {
        val passed = TimeSource.Monotonic.markNow()

        assertNull(awaitRunningHandlers(handlers, passed))
    }

    @Test
    fun `returns null once every running handler has completed`() {
        val gate = CompletableDeferred<Unit>()
        repeat(2) { handlers.launch { gate.await() } }

        val stillActive = awaitInBackground(deadlineIn(5.seconds))
        letTheWaitStart()
        assertFalse(stillActive.isCompleted, "the wait returned while handlers were running")

        gate.complete(Unit)
        assertNull(stillActive.within(2.seconds))
    }

    @Test
    fun `returns the number of handlers still active at the deadline and cancels none`() {
        handlers.launch { delay(50.milliseconds) }
        val hanging = List(2) { launchHangingHandler() }
        val deadline = deadlineIn(200.milliseconds)

        assertEquals(2, awaitRunningHandlers(handlers, deadline))
        assertTrue(deadline.hasPassedNow(), "the wait returned before the deadline")
        assertTrue(hanging.all { it.isActive }, "the wait cancelled a handler")
    }

    @Test
    fun `time spent before the call is taken from the wait`() {
        launchHangingHandler()
        val deadline = deadlineIn(1.seconds)
        Thread.sleep(800)

        val (stillActive, waited) = measureTimedValue { awaitRunningHandlers(handlers, deadline) }

        assertEquals(1, stillActive)
        assertTrue(waited < 700.milliseconds, "waited $waited with 200 ms left")
    }

    @Test
    fun `counts a deadline that has already passed as missed without waiting`() {
        launchHangingHandler()
        val passed = TimeSource.Monotonic.markNow()

        val (stillActive, waited) = measureTimedValue { awaitRunningHandlers(handlers, passed) }

        assertEquals(1, stillActive)
        assertTrue(waited < 1.seconds, "waited $waited for a passed deadline")
    }

    @Test
    fun `counts handlers that are being cancelled as still active`() {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val cancelling = handlers.launch {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) { release.await() }
            }
        }

        try {
            started.within(2.seconds)
            cancelling.cancel()

            assertEquals(1, awaitRunningHandlers(handlers, deadlineIn(100.milliseconds)))
        } finally {
            release.complete(Unit)
        }
    }

    @Test
    fun `failed and cancelled handlers count as completed`() {
        val gate = CompletableDeferred<Unit>()
        handlers.launch {
            gate.await()
            throw IllegalStateException("handler failed")
        }
        val cancelled = launchHangingHandler()

        val stillActive = awaitInBackground(deadlineIn(5.seconds))
        letTheWaitStart()
        gate.complete(Unit)
        cancelled.cancel()

        assertNull(stillActive.within(2.seconds))
    }

    @Test
    fun `handlers started during the wait are not awaited`() {
        val gate = CompletableDeferred<Unit>()
        val late = CompletableDeferred<Job>()
        handlers.launch {
            gate.await()
            late.complete(launchHangingHandler())
        }

        val stillActive = awaitInBackground(deadlineIn(5.seconds))
        letTheWaitStart()
        gate.complete(Unit)

        assertNull(stillActive.within(2.seconds))
        assertTrue(late.within(2.seconds).isActive, "the late handler is not running")
    }

    @Test
    fun `handlers started during the wait are not counted`() {
        val gate = CompletableDeferred<Unit>()
        val late = CompletableDeferred<Job>()
        handlers.launch {
            gate.await()
            late.complete(launchHangingHandler())
            awaitCancellation()
        }

        val stillActive = awaitInBackground(deadlineIn(500.milliseconds))
        letTheWaitStart()
        gate.complete(Unit)

        assertEquals(1, stillActive.within(2.seconds))
        assertTrue(late.within(2.seconds).isActive, "the late handler is not running")
    }

    @Test
    fun `only direct children are counted`() {
        val started = CompletableDeferred<Unit>()
        handlers.launch {
            repeat(2) { launch { awaitCancellation() } }
            started.complete(Unit)
            awaitCancellation()
        }
        started.within(2.seconds)

        assertEquals(1, awaitRunningHandlers(handlers, deadlineIn(200.milliseconds)))
    }

    @Test
    fun `interrupted caller fails with InterruptedException and cancels no handler`() {
        val handler = launchHangingHandler()

        val wait = awaitWithInterruptFlagSet(deadlineIn(5.seconds))

        assertIs<InterruptedException>(wait.result.exceptionOrNull())
        assertFalse(wait.flagStillSet, "the failed wait left the interrupt flag set")
        assertTrue(handler.isActive, "the interrupt cancelled the handler")
    }

    // An interrupted dispatcher drain restores the interrupt flag, so a drain can call the wait with the flag set
    @Test
    fun `returns null and keeps the interrupt flag when no handler is running`() {
        val wait = awaitWithInterruptFlagSet(TimeSource.Monotonic.markNow())

        assertNull(wait.result.getOrThrow())
        assertTrue(wait.flagStillSet, "the wait cleared the interrupt flag")
    }

    @Test
    fun `counts a deadline that has already passed as missed and keeps the interrupt flag`() {
        launchHangingHandler()

        val wait = awaitWithInterruptFlagSet(TimeSource.Monotonic.markNow())

        assertEquals(1, wait.result.getOrThrow())
        assertTrue(wait.flagStillSet, "the wait cleared the interrupt flag")
    }

    private fun launchHangingHandler(): Job = handlers.launch { awaitCancellation() }

    private fun deadlineIn(left: Duration): TimeMark = TimeSource.Monotonic.markNow() + left

    private fun awaitInBackground(deadline: TimeMark): Deferred<Int?> {
        return background.async { awaitRunningHandlers(handlers, deadline) }
    }

    /**
     * Runs the wait on a thread of its own with the interrupt flag set, so that the flag cannot leak into the JUnit
     * worker.
     */
    private fun awaitWithInterruptFlagSet(deadline: TimeMark): InterruptedWait {
        val wait = CompletableDeferred<InterruptedWait>()
        thread(isDaemon = true) {
            Thread.currentThread().interrupt()
            val result = runCatching { awaitRunningHandlers(handlers, deadline) }
            wait.complete(InterruptedWait(result, Thread.currentThread().isInterrupted))
        }
        return wait.within(2.seconds)
    }

    /**
     * Gives the call started by [awaitInBackground] time to take its snapshot of the running handlers.
     */
    private fun letTheWaitStart() = Thread.sleep(100)

    private fun <T> Deferred<T>.within(timeout: Duration): T = runBlocking { withTimeout(timeout) { await() } }

    private class InterruptedWait(val result: Result<Int?>, val flagStillSet: Boolean)
}
