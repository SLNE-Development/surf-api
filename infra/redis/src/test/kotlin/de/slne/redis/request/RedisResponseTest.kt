package de.slne.redis.request

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedisResponseTest {
    @Serializable
    data class TestResponse(val data: String) : RedisResponse()

    @Test
    fun `test response creation includes timestamp`() {
        val response = TestResponse("test data")
        assertTrue(response.timestamp > 0)
        assertTrue(response.timestamp <= System.currentTimeMillis())
    }

    @Test
    fun `test response properties are accessible`() {
        val response = TestResponse("hello world")
        assertEquals("hello world", response.data)
    }
}