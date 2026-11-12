package dev.slne.surf.redis.sync

import dev.slne.surf.redis.sync.StreamVersionTracker.Outcome
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreamVersionTrackerTest {

    @Test
    fun `an un-bootstrapped tracker always demands a resync`() {
        val tracker = StreamVersionTracker()
        assertFalse(tracker.isBootstrapped)
        assertEquals(Outcome.RESYNC, tracker.apply(1))
        assertEquals(Outcome.RESYNC, tracker.applyRange(1, 3))
    }

    @Test
    fun `contiguous versions are applied in order`() {
        val tracker = StreamVersionTracker()
        tracker.bootstrap(4)

        assertEquals(Outcome.APPLIED, tracker.apply(5))
        assertEquals(Outcome.APPLIED, tracker.apply(6))
        assertEquals(6, tracker.current)
    }

    @Test
    fun `already covered versions are skipped without resync`() {
        val tracker = StreamVersionTracker()
        tracker.bootstrap(9)

        assertEquals(Outcome.SKIPPED, tracker.apply(9))
        assertEquals(Outcome.SKIPPED, tracker.apply(3))
        assertEquals(9, tracker.current)
    }

    @Test
    fun `a gap demands a resync and does not advance`() {
        val tracker = StreamVersionTracker()
        tracker.bootstrap(9)

        assertEquals(Outcome.RESYNC, tracker.apply(11))
        assertEquals(9, tracker.current)
    }

    @Test
    fun `a contiguous batch range advances to its last version`() {
        val tracker = StreamVersionTracker()
        tracker.bootstrap(2)

        assertEquals(Outcome.APPLIED, tracker.applyRange(3, 7))
        assertEquals(7, tracker.current)
        assertEquals(Outcome.SKIPPED, tracker.applyRange(3, 7))
    }

    @Test
    fun `a malformed or interleaved range demands a resync`() {
        val tracker = StreamVersionTracker()
        tracker.bootstrap(2)

        assertEquals(Outcome.RESYNC, tracker.applyRange(0, 4))
        assertEquals(Outcome.RESYNC, tracker.applyRange(5, 3))
        assertEquals(Outcome.RESYNC, tracker.applyRange(6, 9))
        assertEquals(2, tracker.current)
    }

    /**
     * The stream poll loop and the Redisson threads completing local mutations both advance the
     * version. With the previous get-then-set implementation two threads could observe the same
     * predecessor and both report APPLIED for the same version, which would dispatch a stream
     * event twice.
     */
    @Test
    fun `exactly one caller applies a given version under contention`() {
        repeat(REPEATS) {
            val tracker = StreamVersionTracker()
            tracker.bootstrap(0)

            val applied = AtomicInteger(0)
            val start = CountDownLatch(1)
            val done = CountDownLatch(THREADS)

            val threads = List(THREADS) {
                Thread {
                    start.await()
                    if (tracker.apply(1) == Outcome.APPLIED) applied.incrementAndGet()
                    done.countDown()
                }.apply { start() }
            }

            start.countDown()
            assertTrue(done.await(30, TimeUnit.SECONDS), "workers did not finish")
            threads.forEach { it.join() }

            assertEquals(1, applied.get(), "version 1 was applied by more than one caller")
            assertEquals(1, tracker.current)
        }
    }

    /**
     * Racing callers must never let the counter skip a version: every version that is reported as
     * APPLIED has to form one contiguous sequence.
     */
    @Test
    fun `concurrent advances never skip or duplicate a version`() {
        repeat(REPEATS) {
            val tracker = StreamVersionTracker()
            tracker.bootstrap(0)

            val appliedCounts = IntArray(VERSIONS + 1)
            val locks = Array(VERSIONS + 1) { Any() }
            val start = CountDownLatch(1)
            val done = CountDownLatch(THREADS)

            val threads = List(THREADS) {
                Thread {
                    start.await()
                    // Every worker offers every version; only one may ever win each.
                    for (version in 1..VERSIONS) {
                        var outcome = tracker.apply(version.toLong())
                        // A RESYNC here just means another worker has not caught up yet.
                        while (outcome == Outcome.RESYNC) {
                            Thread.onSpinWait()
                            outcome = tracker.apply(version.toLong())
                        }
                        if (outcome == Outcome.APPLIED) {
                            synchronized(locks[version]) { appliedCounts[version]++ }
                        }
                    }
                    done.countDown()
                }.apply { start() }
            }

            start.countDown()
            assertTrue(done.await(30, TimeUnit.SECONDS), "workers did not finish")
            threads.forEach { it.join() }

            for (version in 1..VERSIONS) {
                assertEquals(1, appliedCounts[version], "version $version was not applied exactly once")
            }
            assertEquals(VERSIONS.toLong(), tracker.current)
        }
    }

    private companion object {
        const val THREADS = 8
        const val REPEATS = 200
        const val VERSIONS = 64
    }
}
