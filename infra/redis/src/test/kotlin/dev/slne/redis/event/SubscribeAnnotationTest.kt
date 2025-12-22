package dev.slne.redis.event

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertTrue

class SubscribeAnnotationTest {

    @Test
    fun `test Subscribe annotation can be applied to methods`() {
        val method = TestListener::class.java.getDeclaredMethod("onTestEvent", TestEvent::class.java)
        assertTrue(method.isAnnotationPresent(OnRedisEvent::class.java))
    }

    @Serializable
    data class TestEvent(val data: String) : RedisEvent()


    class TestListener {
        @OnRedisEvent
        fun onTestEvent(event: TestEvent) {
            // Test method
        }
    }
}