package dev.slne.surf.nats.connection

import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.TestNatsServer
import io.nats.client.Connection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NatsConnectionManagerTest {

    @Test
    fun `connects and drains`() = runTest {
        NatsInstance.instance.load()
        val connection = NatsConnectionManager.connection()
        assertEquals(Connection.Status.CONNECTED, connection.status)

        NatsInstance.instance.disable()
        assertEquals(Connection.Status.CLOSED, connection.status)
        assertFailsWith<IllegalStateException> { NatsConnectionManager.connection() }
    }

    @Test
    fun `reconnects after server restart`() = runTest(timeout = 60.seconds) {
        NatsInstance.instance.load()
        try {
            val connection = NatsConnectionManager.connection()
            val reconnectsBefore = connection.statistics.reconnects

            withContext(Dispatchers.IO) { TestNatsServer.restart() }

            awaitCondition("reconnect after server restart") {
                connection.status == Connection.Status.CONNECTED &&
                        connection.statistics.reconnects > reconnectsBefore
            }

            assertSame(connection, NatsConnectionManager.connection())
            withContext(Dispatchers.IO) { connection.flush(Duration.ofSeconds(5)) }
        } finally {
            NatsInstance.instance.disable()
        }
    }

    private suspend fun awaitCondition(description: String, condition: () -> Boolean) {
        val reached = withContext(Dispatchers.Default) {
            withTimeoutOrNull(30.seconds) {
                while (!condition()) delay(100.milliseconds)
            }
        }
        assertNotNull(reached, "Timed out waiting for $description")
    }
}
