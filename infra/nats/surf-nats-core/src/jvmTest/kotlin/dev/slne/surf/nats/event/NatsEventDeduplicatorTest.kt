package dev.slne.surf.nats.event

import dev.slne.surf.nats.InternalNatsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class NatsEventDeduplicatorTest {

    private val time = TestTimeSource()

    @Test
    fun `a repeated event id is not a first delivery`() {
        val deduplicator = NatsEventDeduplicator(timeSource = time)

        assertTrue(deduplicator.firstDelivery("event-1"))
        assertFalse(deduplicator.firstDelivery("event-1"))
        assertTrue(deduplicator.firstDelivery("event-2"))
    }

    @Test
    fun `an event id is forgotten after the retention since its first delivery`() {
        val deduplicator = NatsEventDeduplicator(retention = 1.minutes, timeSource = time)
        assertTrue(deduplicator.firstDelivery("event-1"))

        time += 59.seconds
        assertFalse(deduplicator.firstDelivery("event-1"), "a repeat does not extend the retention")
        time += 1.seconds
        assertTrue(deduplicator.firstDelivery("event-1"))
    }

    @Test
    fun `the oldest event id is forgotten when the deduplicator is full`() {
        val deduplicator = NatsEventDeduplicator(maxEntries = 2, timeSource = time)
        deduplicator.firstDelivery("event-1")
        deduplicator.firstDelivery("event-2")
        deduplicator.firstDelivery("event-3")

        assertFalse(deduplicator.firstDelivery("event-3"))
        assertFalse(deduplicator.firstDelivery("event-2"))
        assertTrue(deduplicator.firstDelivery("event-1"))
    }

    @Test
    fun `concurrent deliveries of one event id have exactly one first delivery`() = runBlocking {
        val deduplicator = NatsEventDeduplicator()

        val firsts = (1..64).map {
            async(Dispatchers.Default) { (1..100).count { id -> deduplicator.firstDelivery("event-$id") } }
        }.awaitAll()

        assertEquals(100, firsts.sum())
    }

    @OptIn(InternalNatsApi::class)
    @Test
    fun `the listener variant reads the event id of the listener context`() = runBlocking<Unit> {
        val deduplicator = NatsEventDeduplicator(timeSource = time)

        withContext(NatsEventContext(origin = null, isFromThisClient = false, eventId = "event-1")) {
            assertTrue(deduplicator.firstDelivery())
            assertFalse(deduplicator.firstDelivery())
        }
        withContext(NatsEventContext(origin = null, isFromThisClient = false, eventId = null)) {
            assertTrue(deduplicator.firstDelivery(), "an event without id cannot be recognized")
            assertTrue(deduplicator.firstDelivery())
        }
        assertFailsWith<IllegalStateException> { deduplicator.firstDelivery() }
    }

    @Test
    fun `limits must be positive`() {
        assertFailsWith<IllegalArgumentException> { NatsEventDeduplicator(maxEntries = 0) }
        assertFailsWith<IllegalArgumentException> { NatsEventDeduplicator(retention = Duration.ZERO) }
    }
}
