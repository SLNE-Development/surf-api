package dev.slne.surf.nats.client

import dev.slne.surf.nats.NatsClient
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlin.test.*

class NatsClientTest {

    @Test
    fun `accepts kebab-case names`() {
        for (name in listOf("transaction", "player-data", "shop2", "a-b-c")) {
            NatsClient.create(name).use { assertEquals(name, it.name) }
        }
    }

    @Test
    fun `rejects invalid names`() {
        for (name in listOf(
            "",
            "Transaction",
            "player_data",
            "a.b",
            "a b",
            "-shop",
            "shop-",
            "a--b",
            "a*",
            "a>"
        )) {
            assertFailsWith<IllegalArgumentException>("name '$name'") { NatsClient.create(name) }
        }
    }

    @Test
    fun `rejects duplicate names until the first client is closed`() {
        val first = NatsClient.create("duplicate")
        first.use {
            assertFailsWith<IllegalStateException> { NatsClient.create("duplicate") }
        }

        NatsClient.create("duplicate").close()
    }

    @Test
    fun `close is idempotent and marks the client closed`() {
        val client = NatsClient.create("close-twice")
        assertFalse(client.isClosed)

        client.close()
        client.close()

        assertTrue(client.isClosed)
    }

    @Test
    fun `closed client rejects further use`() {
        val client = NatsClient.create("closed-use")
        client.close()

        assertFailsWith<IllegalStateException> { client.publish(Any()) }
        assertFailsWith<IllegalStateException> { client.registerListener(Any()) }
    }

    @Test
    fun `close cancels the client scope`() {
        val client = NatsClient.create("scope-cancel") as NatsClientImpl
        val job = client.clientScope.launch { awaitCancellation() }

        client.close()

        assertTrue(job.isCancelled)
    }
}
