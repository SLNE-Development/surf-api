package dev.slne.surf.microservice.database.health

import dev.slne.surf.microservice.api.health.MicroserviceHealthStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class DatabaseHealthCheckTest {

    @Test
    fun `reports healthy when the probe completes`() = runTest {
        val result = checkDatabaseHealth(COMPONENT, TIMEOUT) {}

        assertEquals(MicroserviceHealthStatus.HEALTHY, result.status)
        assertEquals(COMPONENT, result.component)
    }

    @Test
    fun `reports unhealthy with the exception message`() = runTest {
        val result = checkDatabaseHealth(COMPONENT, TIMEOUT) { error("connection refused") }

        assertEquals(MicroserviceHealthStatus.UNHEALTHY, result.status)
        assertEquals("connection refused", result.message)
    }

    @Test
    fun `falls back to the exception type without a message`() = runTest {
        val result = checkDatabaseHealth(COMPONENT, TIMEOUT) { throw IllegalStateException() }

        assertEquals(IllegalStateException::class.java.name, result.message)
    }

    @Test
    fun `reports unhealthy when the probe throws an error`() = runTest {
        val result = checkDatabaseHealth(COMPONENT, TIMEOUT) { throw NoClassDefFoundError("driver") }

        assertEquals(MicroserviceHealthStatus.UNHEALTHY, result.status)
        assertEquals("driver", result.message)
    }

    @Test
    fun `reports unhealthy when the probe times out`() = runTest {
        val result = checkDatabaseHealth(COMPONENT, TIMEOUT) { delay(TIMEOUT * 2) }

        assertEquals(MicroserviceHealthStatus.UNHEALTHY, result.status)
        assertEquals("Database health check timed out after $TIMEOUT", result.message)
    }

    @Test
    fun `propagates cancellation`() = runTest {
        assertFailsWith<CancellationException> {
            checkDatabaseHealth(COMPONENT, TIMEOUT) { throw CancellationException("cancelled") }
        }
    }

    private companion object {
        const val COMPONENT = "database"
        val TIMEOUT = 2.5.seconds
    }
}
