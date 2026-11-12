package dev.slne.surf.nats.rpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class NotAdmittedLogTest {

    private val lines = mutableListOf<String>()

    @Test
    fun `first rejection is reported at once and later ones are summarized per interval`() = runTest {
        val log = notAdmittedLog(backgroundScope)

        repeat(3) { log.report("surf.rpc.test.a") }
        assertEquals(1, lines.size)
        assertTrue("surf.rpc.test.a" in lines[0], lines[0])

        advanceTimeBy(INTERVAL + 1.seconds)
        assertEquals(summary(2), lines[1])

        log.report("surf.rpc.test.b")
        advanceTimeBy(INTERVAL)
        assertEquals(summary(1), lines[2])
        assertEquals(3, lines.size, "$lines")
    }

    @Test
    fun `rejection after a quiet interval is reported at once`() = runTest {
        val log = notAdmittedLog(backgroundScope)
        log.report("surf.rpc.test.a")

        advanceTimeBy(INTERVAL * 3)
        log.report("surf.rpc.test.b")

        assertEquals(2, lines.size, "$lines")
        assertTrue("surf.rpc.test.b" in lines[1], lines[1])
    }

    @Test
    fun `pending summary is reported when the scope is cancelled`() = runTest {
        val summaries = CoroutineScope(StandardTestDispatcher(testScheduler) + Job())
        val log = notAdmittedLog(summaries)
        repeat(2) { log.report("surf.rpc.test.a") }
        runCurrent()

        summaries.cancel()
        runCurrent()

        assertEquals(listOf(summary(1)), lines.drop(1))
    }

    private fun notAdmittedLog(scope: CoroutineScope) =
        NotAdmittedLog("test.Service", permits = 2, scope = scope, interval = INTERVAL, warn = { lines += it })

    private fun summary(count: Int) = "$count more requests to test.Service got no handler slot in the last $INTERVAL"

    private companion object {
        val INTERVAL = 10.seconds
    }
}
