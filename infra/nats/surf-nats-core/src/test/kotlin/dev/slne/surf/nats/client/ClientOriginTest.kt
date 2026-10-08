package dev.slne.surf.nats.client

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.internal.NatsClientFactory
import kotlinx.serialization.modules.EmptySerializersModule
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClientOriginTest {

    @Test
    fun `origin points to the caller of NatsClient create`() {
        NatsClient.create("origin-create").use { client ->
            assertCallerIsThisTest((client as NatsClientImpl).origin)
        }
    }

    @Test
    fun `origin points to the caller of the factory`() {
        NatsClientFactory.instance.create("origin-factory", EmptySerializersModule()).use { client ->
            assertCallerIsThisTest((client as NatsClientImpl).origin)
        }
    }

    @Test
    fun `origin records time and thread of creation`() {
        val before = Instant.now()
        NatsClient.create("origin-meta").use { client ->
            val origin = (client as NatsClientImpl).origin
            assertTrue(!origin.createdAt.isBefore(before) && !origin.createdAt.isAfter(Instant.now()))
            assertEquals(Thread.currentThread().name, origin.thread)
        }
    }

    @Test
    fun `source names the caller class and its location`() {
        NatsClient.create("origin-source").use { client ->
            val source = (client as NatsClientImpl).origin.source
            val location = ClientOriginTest::class.java.protectionDomain.codeSource.location
            assertEquals("${ClientOriginTest::class.java.name} ($location)", source)
        }
    }

    @Test
    fun `stack trace is limited`() {
        NatsClient.create("origin-limit").use { client ->
            val origin = (client as NatsClientImpl).origin
            assertTrue(origin.stackTrace.size in 1..12, "frames: ${origin.stackTrace.size}")
        }
    }

    private fun assertCallerIsThisTest(origin: ClientOrigin) {
        val first = origin.stackTrace.first()
        assertEquals(ClientOriginTest::class.java.name, first.className, "first frame: $first")
        val internal = setOf(
            NatsClientFactoryImpl::class.java.name,
            NatsClient.Factory::class.java.name,
            ClientOrigin::class.java.name,
            ClientOrigin.Companion::class.java.name,
        )
        assertTrue(origin.stackTrace.none { it.className in internal }, "frames: ${origin.stackTrace}")
    }
}
