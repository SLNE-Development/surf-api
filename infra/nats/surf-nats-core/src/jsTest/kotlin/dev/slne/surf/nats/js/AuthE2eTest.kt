package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.ConnectOptions
import dev.slne.surf.nats.js.api.ListenOptions
import dev.slne.surf.nats.js.api.PublishOptions
import dev.slne.surf.nats.js.fixtures.ChatMessage
import dev.slne.surf.nats.js.fixtures.ChatMessageSchema
import dev.slne.surf.nats.js.fixtures.fields
import js.objects.unsafeJso
import kotlinx.coroutines.await
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Token and user/password authentication against the servers `startNatsJsTestServer` starts with `--auth` and
 * `--user`/`--pass`.
 */
class AuthE2eTest {

    private fun options(url: String, token: String? = null, user: String? = null, pass: String? = null) = ConnectOptions(
        name = uniqueName("auth-client"),
        servers = arrayOf(url),
        connectTimeoutMs = 2000,
        token = token,
        user = user,
        pass = pass,
    )

    // Publishes and receives one event, which needs an authorized connection.
    private suspend fun assertUsable(client: SurfNatsClient) {
        try {
            assertFalse(client.isClosed)
            val owner = uniqueName("auth-owner")
            val received = mutableListOf<String?>()
            client.listen(ChatMessageSchema, { event, _ -> received.add(event.fields<ChatMessage>().text) }, ListenOptions(owner = owner)).await()
            client.publish(ChatMessageSchema, unsafeJso<ChatMessage> { text = "authorized" }, PublishOptions(owner = owner)).await()
            eventually { received.size == 1 }
            assertEquals(listOf<String?>("authorized"), received)
        } finally {
            client.close().await()
        }
    }

    private suspend fun assertRejected(options: ConnectOptions) {
        val error = assertFails { connect(options).await().close().await() }
        val text = "${error.message} ${error.cause?.message}".lowercase()
        assertTrue("authorization" in text, "expected an authorization error, got $error")
    }

    @Test
    fun tokenAuthentication() = e2eTest {
        val url = TestNats.auth("tokenUrl")
        assertUsable(connect(options(url, token = TestNats.auth("token"))).await())
        assertRejected(options(url, token = "wrong-token"))
        assertRejected(options(url))
    }

    @Test
    fun userAndPasswordAuthentication() = e2eTest {
        val url = TestNats.auth("userUrl")
        assertUsable(connect(options(url, user = TestNats.auth("user"), pass = TestNats.auth("pass"))).await())
        assertRejected(options(url, user = TestNats.auth("user"), pass = "wrong-pass"))
        assertRejected(options(url))
    }
}
