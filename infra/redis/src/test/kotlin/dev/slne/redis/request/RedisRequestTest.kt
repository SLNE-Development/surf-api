package dev.slne.redis.request

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedisRequestTest {

    @Serializable
    data class TestRequest(val message: String, val value: Int) : RedisRequest()

    @Test
    fun `test request creation includes timestamp`() {
        val request = TestRequest("test", 42)
        assertTrue(request.timestamp > 0)
        assertTrue(request.timestamp <= System.currentTimeMillis())
    }

    @Test
    fun `test request properties are accessible`() {
        val request = TestRequest("hello", 123)
        assertEquals("hello", request.message)
        assertEquals(123, request.value)
    }

}