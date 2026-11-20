package dev.slne.surf.microservice.database.health

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.springframework.boot.health.contributor.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class DatabaseHealthCheckTest {

    @Test
    fun `reports up when the probe completes`() = runTest {
        val health = checkDatabaseHealth(TIMEOUT) {}

        assertEquals(Status.UP, health.status)
        assertTrue(health.details.isEmpty())
    }

    @Test
    fun `reports down with the exception message`() = runTest {
        val health = checkDatabaseHealth(TIMEOUT) { error("connection refused") }

        assertEquals(Status.DOWN, health.status)
        assertEquals("connection refused", health.details["error"])
    }

    @Test
    fun `falls back to the exception type without a message`() = runTest {
        val health = checkDatabaseHealth(TIMEOUT) { throw IllegalStateException() }

        assertEquals(IllegalStateException::class.java.name, health.details["error"])
    }

    @Test
    fun `reports down when the probe throws an error`() = runTest {
        val health = checkDatabaseHealth(TIMEOUT) { throw NoClassDefFoundError("driver") }

        assertEquals(Status.DOWN, health.status)
        assertEquals("driver", health.details["error"])
    }

    @Test
    fun `reports down when the probe times out`() = runTest {
        val health = checkDatabaseHealth(TIMEOUT) { delay(TIMEOUT * 2) }

        assertEquals(Status.DOWN, health.status)
        assertEquals("Database health check timed out after $TIMEOUT", health.details["error"])
    }

    @Test
    fun `propagates cancellation`() = runTest {
        assertFailsWith<CancellationException> {
            checkDatabaseHealth(TIMEOUT) { throw CancellationException("cancelled") }
        }
    }

    private companion object {
        val TIMEOUT = 2.5.seconds
    }
}
