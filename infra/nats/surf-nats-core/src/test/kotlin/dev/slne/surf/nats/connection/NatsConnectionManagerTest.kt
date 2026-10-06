package dev.slne.surf.nats.connection

import dev.slne.surf.nats.NatsInstance
import io.nats.client.Connection
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
