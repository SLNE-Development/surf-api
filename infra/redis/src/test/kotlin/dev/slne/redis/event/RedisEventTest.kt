package dev.slne.redis.event

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class RedisEventTest {
    @Serializable
    data class TestEvent(val message: String, val value: Int) : RedisEvent()

    @Test
    fun `test event creation includes timestamp`() {
        val event = TestEvent("test", 42)
        assertTrue(event.timestamp > 0)
        assertTrue(event.timestamp <= System.currentTimeMillis())
    }

    @Test
    fun `test event properties are accessible`() {
        val event = TestEvent("hello", 123)
        assertEquals("hello", event.message)
        assertEquals(123, event.value)
    }
}