package dev.slne.redis.event.sub

import dev.slne.redis.RedisApi
import dev.slne.redis.RedisTestBase
import dev.slne.redis.event.OnRedisEvent
import dev.slne.redis.event.RedisEvent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterEach
import kotlin.test.Test
import kotlin.test.assertEquals

class RedisEventBusTest: RedisTestBase() {

    @Serializable
    data class TestEvent(val message: String) : RedisEvent()

    override fun beforeApiFreeze(api: RedisApi) {
        api.subscribeToEvents(TestListener)
    }

    @AfterEach
    fun cleanup() {
        TestListener.receivedEvents.clear()
    }

    @Test
    fun testEventBusListenerReceivesEvent() = runTest {
        val event = TestEvent("Hello, Event Bus!")
        val received = redisApi.publishEvent(event).await()

        assertEquals(1, received, "Event should be received by one listener")
        assertEquals(1, TestListener.receivedEvents.size, "Listener should have received one event")
        assertEquals(
            event,
            TestListener.receivedEvents[0],
            "Received event should match the published event"
        )
    }

    object TestListener {
        val receivedEvents = mutableListOf<TestEvent>()

        @OnRedisEvent
        fun onTestEvent(event: TestEvent) {
            receivedEvents.add(event)
        }
    }
}