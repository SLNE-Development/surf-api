package dev.slne.surf.nats.event

import kotlinx.coroutines.*
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class NatsEventDeduplicatorTest {

    private val time = TestTimeSource()

    @Test
    fun `a repeated event id is not a first delivery`() {
        val deduplicator = NatsEventDeduplicator(timeSource = time)

        assertTrue(deduplicator.isFirstDelivery("event-1"))
        assertFalse(deduplicator.isFirstDelivery("event-1"))
        assertTrue(deduplicator.isFirstDelivery("event-2"))
    }

    @Test
    fun `an event id is forgotten after the retention since its first delivery`() {
        val deduplicator = NatsEventDeduplicator(retention = 1.minutes, timeSource = time)
        assertTrue(deduplicator.isFirstDelivery("event-1"))

        time += 59.seconds
        assertFalse(
            deduplicator.isFirstDelivery("event-1"),
            "a repeat does not extend the retention",
        )
        time += 1.seconds
        assertTrue(deduplicator.isFirstDelivery("event-1"))
    }

    @Test
    fun `the oldest event id is forgotten when the deduplicator is full`() {
        val deduplicator = NatsEventDeduplicator(maxEntries = 2, timeSource = time)
        deduplicator.isFirstDelivery("event-1")
        deduplicator.isFirstDelivery("event-2")
        deduplicator.isFirstDelivery("event-3")

        assertFalse(deduplicator.isFirstDelivery("event-3"))
        assertFalse(deduplicator.isFirstDelivery("event-2"))
        assertTrue(deduplicator.isFirstDelivery("event-1"))
    }

    @Test
    fun `concurrent deliveries of one event id have exactly one first delivery`() = runBlocking {
        val deduplicator = NatsEventDeduplicator()

        val firsts = (1..64)
            .map {
                async(Dispatchers.Default) {
                    (1..100).count { id -> deduplicator.isFirstDelivery("event-$id") }
                }
            }
            .awaitAll()

        assertEquals(100, firsts.sum())
    }

    @Test
    fun `the listener variant reads the event id of the listener context`() = runBlocking<Unit> {
        val deduplicator = NatsEventDeduplicator(timeSource = time)

        withContext(listenerContext(eventId = "event-1")) {
            assertTrue(deduplicator.isFirstDelivery())
            assertFalse(deduplicator.isFirstDelivery())
        }
        withContext(listenerContext(eventId = null)) {
            assertTrue(deduplicator.isFirstDelivery(), "an event without id cannot be recognized")
            assertTrue(deduplicator.isFirstDelivery())
        }
        assertFailsWith<IllegalStateException> { deduplicator.isFirstDelivery() }
    }

    @Test
    fun `limits must be positive`() {
        assertFailsWith<IllegalArgumentException> { NatsEventDeduplicator(maxEntries = 0) }
        assertFailsWith<IllegalArgumentException> {
            NatsEventDeduplicator(retention = Duration.ZERO)
        }
    }

    private fun listenerContext(eventId: String?): NatsEventContext {
        return NatsEventContext(origin = null, isFromThisClient = false, eventId = eventId)
    }
}
