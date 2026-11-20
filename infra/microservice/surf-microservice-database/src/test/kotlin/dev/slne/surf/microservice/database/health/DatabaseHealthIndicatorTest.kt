package dev.slne.surf.microservice.database.health

import kotlinx.coroutines.delay
import org.springframework.boot.health.contributor.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

class DatabaseHealthIndicatorTest {

    @Test
    fun `is up when the probe succeeds`() {
        val health = DatabaseHealthIndicator {}.health()

        assertEquals(Status.UP, health.status)
    }

    @Test
    fun `is down with an error detail when the probe fails`() {
        val health = DatabaseHealthIndicator { error("connection refused") }.health()

        assertEquals(Status.DOWN, health.status)
        assertEquals("connection refused", health.details["error"])
    }

    @Test
    fun `is down when the probe exceeds the timeout`() {
        val health = DatabaseHealthIndicator(timeout = 50.milliseconds) { delay(5_000) }.health()

        assertEquals(Status.DOWN, health.status)
    }

    @Test
    fun `omits the details on request`() {
        val health = DatabaseHealthIndicator { error("connection refused") }.health(false)

        assertEquals(Status.DOWN, health?.status)
        assertEquals(emptyMap(), health?.details)
    }
}
