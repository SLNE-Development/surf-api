@file:OptIn(DelicateCoroutinesApi::class)

package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.EventContext
import dev.slne.surf.nats.js.api.ProtoMessage
import dev.slne.surf.nats.js.client.JsRuntimeDefaults
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.fixtures.ChatMessage
import dev.slne.surf.nats.js.fixtures.ChatMessageSchema
import dev.slne.surf.nats.js.fixtures.FindPlayerRequest
import dev.slne.surf.nats.js.fixtures.FindPlayerResponse
import dev.slne.surf.nats.js.fixtures.FindPlayerResponseSchema
import dev.slne.surf.nats.js.fixtures.PlayerJoined
import dev.slne.surf.nats.js.fixtures.PlayerJoinedSchema
import dev.slne.surf.nats.js.fixtures.PlayerService
import dev.slne.surf.nats.js.fixtures.Uuid
import dev.slne.surf.nats.js.fixtures.fields
import dev.slne.surf.nats.runtime.epochMillis
import js.objects.unsafeJso
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import kotlinx.coroutines.withTimeout
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CloseE2eTest {

    private fun findPlayer(): DescMethod = PlayerService.methods.first { it.localName == "findPlayer" }

    // A listener handler that suspends until release() and then runs body.
    private class GatedHandler(private val body: suspend () -> Unit = {}) {
        private val gate = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()
        val finished = CompletableDeferred<Unit>()

        val handler: (ProtoMessage, EventContext) -> Any? = { _, _ ->
            GlobalScope.promise {
                entered.complete(Unit)
                gate.await()
                try {
                    body()
                    finished.complete(Unit)
                } catch (e: Throwable) {
                    finished.completeExceptionally(e)
                    throw e
                }
            }
        }

        fun release() {
            gate.complete(Unit)
        }
    }

    // Runs the gated handler on client and starts closing client while it runs; returns the pending close().
    private suspend fun closeWhileRunning(
        client: SurfNatsClient,
        publisher: SurfNatsClient,
        gated: GatedHandler,
    ): Promise<Unit> {
        client.listen(ChatMessageSchema, gated.handler).await()
        publisher.publish(ChatMessageSchema, unsafeJso<ChatMessage> { text = "close" }).await()
        withTimeout(5.seconds) { gated.entered.await() }

        val closing = client.close()
        // Lets close() run up to the point where it waits for the handler.
        delay(200.milliseconds)
        return closing
    }

    @Test
    fun handlersRunningWhileTheClientClosesCanStillPublishAndCall() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("close-handler"))
        val publisher = TestNats.client(uniqueName("close-publisher"))
        try {
            val published = mutableListOf<String?>()
            raw.subscribe("surf.evt.surf.natsjs.test.v1.PlayerJoined") { message ->
                published += raw.decode(PlayerJoinedSchema, message.data).fields<PlayerJoined>().name
            }
            raw.subscribe("surf.rpc.surf.natsjs.test.v1.PlayerService.FindPlayer", queue = "surf.natsjs.test.v1.PlayerService") { request ->
                raw.respond(request, raw.encode(FindPlayerResponseSchema, unsafeJso<FindPlayerResponse> {
                    name = "Steve"
                    server = "lobby"
                }))
            }
            raw.flush()

            var server: String? = null
            val gated = GatedHandler {
                val joined = unsafeJso<PlayerJoined> {
                    name = "during-close"
                    id = unsafeJso<Uuid> { value = bytesOfSize(16) }
                }
                client.publish(PlayerJoinedSchema, joined).await()
                val request = unsafeJso<FindPlayerRequest> { name = "Steve" }
                server = client.call(findPlayer(), request).await().fields<FindPlayerResponse>().server
            }
            val closing = closeWhileRunning(client, publisher, gated)

            assertFalse(client.isClosed, "client counts as closed while it drains")
            gated.release()
            closing.await()

            gated.finished.await()
            assertTrue(client.isClosed)
            assertEquals("lobby", server)
            eventually { published.isNotEmpty() }
            assertEquals(listOf<String?>("during-close"), published)
        } finally {
            client.close().await()
            publisher.close().await()
            raw.close()
        }
    }

    @Test
    fun closeWaitsForRunningHandlers() = e2eTest {
        val client = TestNats.client(uniqueName("close-handler"))
        val publisher = TestNats.client(uniqueName("close-publisher"))
        try {
            val gated = GatedHandler()
            var resolved = false
            val closing = closeWhileRunning(client, publisher, gated).then { resolved = true }

            delay(300.milliseconds)
            assertFalse(resolved, "close() resolved while a handler was still running")
            gated.release()
            closing.await()

            assertTrue(gated.finished.isCompleted)
            assertTrue(client.isClosed)
        } finally {
            client.close().await()
            publisher.close().await()
        }
    }

    @Test
    fun closeStopsWaitingForAHandlerThatNeverSettlesAfterTheDrainTimeout() = e2eTest {
        val client = TestNats.client(uniqueName("close-handler"))
        val publisher = TestNats.client(uniqueName("close-publisher"))
        val previousTimeout = JsRuntimeDefaults.drainTimeout
        try {
            JsRuntimeDefaults.drainTimeout = 500.milliseconds
            val gated = GatedHandler()
            val start = epochMillis()
            val closing = closeWhileRunning(client, publisher, gated)

            withTimeout(5.seconds) { closing.await() }
            val took = epochMillis() - start

            assertTrue(client.isClosed)
            assertFalse(gated.finished.isCompleted, "the handler was never released")
            assertTrue(took in 500..4000, "close() took $took ms")
        } finally {
            JsRuntimeDefaults.drainTimeout = previousTimeout
            client.close().await()
            publisher.close().await()
        }
    }

    @Test
    fun listenIsRejectedWhileClosingAndAfterClose() = e2eTest {
        val client = TestNats.client(uniqueName("close-handler"))
        val publisher = TestNats.client(uniqueName("close-publisher"))
        try {
            val gated = GatedHandler()
            val closing = closeWhileRunning(client, publisher, gated)

            val whileClosing = assertFailsWith<IllegalStateException> {
                client.listen(ChatMessageSchema, { _, _ -> }).await()
            }
            assertEquals("NatsClient[${client.name}] is closing", whileClosing.message)

            gated.release()
            closing.await()

            val afterClose = assertFailsWith<IllegalStateException> {
                client.listen(ChatMessageSchema, { _, _ -> }).await()
            }
            assertEquals("NatsClient[${client.name}] is closed", afterClose.message)
        } finally {
            client.close().await()
            publisher.close().await()
        }
    }
}
