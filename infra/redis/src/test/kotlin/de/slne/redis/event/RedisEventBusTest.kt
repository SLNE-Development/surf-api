package de.slne.redis.event

import kotlinx.coroutines.runBlocking
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

class SubscribeAnnotationTest {
    
    @Test
    fun `test Subscribe annotation can be applied to methods`() {
        val method = TestListener::class.java.getDeclaredMethod("onTestEvent", TestEvent::class.java)
        assertTrue(method.isAnnotationPresent(Subscribe::class.java))
    }
    
    @Serializable
    data class TestEvent(val data: String) : RedisEvent()
    
    class TestListener {
        @Subscribe
        fun onTestEvent(event: TestEvent) {
            // Test method
        }
    }
}

class RedisEventBusTest {
    
    @Serializable
    data class TestEvent(val message: String) : RedisEvent()
    
    @Test
    fun `test listener registration scans Subscribe methods`() = runBlocking {
        // This test verifies that listener registration doesn't throw exceptions
        // Full integration testing requires a running Redis instance
        val listener = TestListener()
        
        // Note: We can't fully test without a Redis connection
        // but we can verify the API works
        try {
            // This will fail to connect but validates the API
            val eventBus = RedisEventBus("redis://localhost:6379")
            eventBus.registerListener(listener)
            eventBus.unregisterListener(listener)
            eventBus.close()
        } catch (e: Exception) {
            // Expected when Redis is not running
            assertTrue(e.message?.contains("Unable to connect") == true || 
                       e.message?.contains("Connection refused") == true ||
                       e.cause?.message?.contains("Connection refused") == true)
        }
    }
    
    class TestListener {
        var receivedEvents = mutableListOf<TestEvent>()
        
        @Subscribe
        fun onTestEvent(event: TestEvent) {
            receivedEvents.add(event)
        }
    }
}
