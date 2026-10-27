package dev.slne.surf.nats.js

import dev.slne.surf.nats.js.api.MethodSchema
import dev.slne.surf.nats.js.codec.emptyBody
import dev.slne.surf.nats.js.error.NatsCallTimeoutError
import dev.slne.surf.nats.js.error.NatsNoRespondersError
import dev.slne.surf.nats.js.error.NatsRemoteError
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.protobuf.DescMethod
import dev.slne.surf.nats.js.external.protobuf.DescService
import dev.slne.surf.nats.js.fixtures.FindPlayerRequest
import dev.slne.surf.nats.js.fixtures.FindPlayerRequestSchema
import dev.slne.surf.nats.js.fixtures.FindPlayerResponse
import dev.slne.surf.nats.js.fixtures.FindPlayerResponseSchema
import dev.slne.surf.nats.js.fixtures.LocateResponse
import dev.slne.surf.nats.js.fixtures.LocateResponseSchema
import dev.slne.surf.nats.js.fixtures.PlayerService
import dev.slne.surf.nats.js.fixtures.RegistryService
import dev.slne.surf.nats.js.fixtures.fields
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.runtime.epochMillis
import js.objects.unsafeJso
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class RpcE2eTest {

    private fun method(service: DescService, name: String): DescMethod = service.methods.first { it.localName == name }

    private fun options(owner: String?, timeoutMs: Int? = null) = callOptions(owner, timeoutMs)

    private fun findRequest(name: String? = null): FindPlayerRequest = unsafeJso { if (name != null) this.name = name }

    private fun subject(owner: String, method: String) = "$owner.\$rpc.v1.players.$method"

    @Test
    fun coreCallSendsTheProtocolHeadersAndDecodesTheResponse() = e2eTest {
        val owner = uniqueName("rpc-owner")
        val raw = rawNats()
        val client = TestNats.client(uniqueName("rpc-caller"))
        try {
            val requests = mutableListOf<Msg>()
            raw.subscribe(subject(owner, "find-player"), queue = owner) { request ->
                requests += request
                val name = raw.decode(FindPlayerRequestSchema, request.data).fields<FindPlayerRequest>().name
                raw.respond(request, raw.encode(FindPlayerResponseSchema, unsafeJso<FindPlayerResponse> {
                    this.name = name
                    server = "lobby"
                }))
            }
            raw.flush()

            val before = epochMillis()
            val response = client.target(owner)
                .call(method(PlayerService, "findPlayer"), findRequest("Steve"))
                .await()
                .fields<FindPlayerResponse>()
            assertEquals("Steve", response.name)
            assertEquals("lobby", response.server)

            val request = requests.single()
            assertEquals(ProtocolHeaders.PROTOBUF_CONTENT_TYPE, header(request, ProtocolHeaders.CONTENT_TYPE))
            assertEquals(client.origin, header(request, ProtocolHeaders.ORIGIN))
            val deadline = header(request, ProtocolHeaders.DEADLINE)!!.toLong()
            assertTrue(deadline >= before + 4_000 && deadline <= epochMillis() + 5_000, "deadline $deadline")
            assertNull(header(request, ProtocolHeaders.PAYLOAD_REF))
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun surfErrorBecomesNatsRemoteErrorAndEmptyResponsesAreUnit() = e2eTest {
        val owner = uniqueName("rpc-owner")
        val raw = rawNats()
        val client = TestNats.client(uniqueName("rpc-caller"))
        try {
            raw.subscribe(subject(owner, "find-player"), queue = owner) { request ->
                val headers = raw.jvmHeaders()
                headers.set(ProtocolHeaders.ERROR, "java.lang.IllegalStateException")
                raw.respond(request, utf8Bytes("player is offline"), headers)
            }
            raw.subscribe(subject(owner, "kick"), queue = owner) { request -> raw.respond(request, emptyBody()) }
            raw.flush()

            val error = assertFails {
                client.call(method(PlayerService, "findPlayer"), findRequest("x"), options(owner)).await()
            }
            val remote = assertIs<NatsRemoteError>(error)
            assertEquals("java.lang.IllegalStateException", remote.type)
            assertEquals("player is offline", remote.remoteMessage)

            val kicked = client.call(method(PlayerService, "kick"), findRequest("x"), options(owner)).await()
            assertEquals("undefined", jsTypeOf(kicked))
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun noRespondersAndTimeout() = e2eTest {
        val owner = uniqueName("rpc-owner")
        val raw = rawNats()
        val client = TestNats.client(uniqueName("rpc-caller"))
        try {
            val noResponders = assertFails {
                client.call(method(PlayerService, "findPlayer"), findRequest("x"), options(owner)).await()
            }
            assertEquals(subject(owner, "find-player"), assertIs<NatsNoRespondersError>(noResponders).subject)

            raw.subscribe(subject(owner, "find-player"), queue = owner) { }
            raw.flush()
            val started = epochMillis()
            val timeout = assertFails {
                client.call(method(PlayerService, "findPlayer"), findRequest("x"), options(owner, 300)).await()
            }
            val timeoutError = assertIs<NatsCallTimeoutError>(timeout)
            assertEquals(300, timeoutError.timeoutMs)
            assertEquals(subject(owner, "find-player"), timeoutError.subject)
            assertTrue(epochMillis() - started < 3_000)

            // The contract timeout (call_timeout_seconds: 1) applies without an option.
            raw.subscribe(subject(owner, "slow-find"), queue = owner) { }
            raw.flush()
            val contractTimeout = assertFails {
                client.call(method(PlayerService, "slowFind"), findRequest("x"), options(owner)).await()
            }
            assertEquals(1000, assertIs<NatsCallTimeoutError>(contractTimeout).timeoutMs)
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun callValidatesTheRequestAndTheMethodKind() = e2eTest {
        val client = TestNats.client(uniqueName("rpc-caller"))
        try {
            val missing = assertFails {
                client.call(method(PlayerService, "findPlayer"), findRequest(), options("surf-punish")).await()
            }
            assertTrue(missing.message!!.contains("'name'"), missing.message)

            val answerMethod = assertFails {
                client.call(method(PlayerService, "locate"), findRequest("x"), options("surf-punish")).await()
            }
            assertTrue(answerMethod.message!!.contains("use callAnswer"), answerMethod.message)

            val valueMethod = assertFails {
                client.callAnswer(method(PlayerService, "findPlayer"), findRequest("x"), options("surf-punish")).await()
            }
            assertTrue(valueMethod.message!!.contains("use call"), valueMethod.message)

            assertFailsWith<IllegalArgumentException> {
                client.call(FindPlayerRequestSchema.unsafeCast<MethodSchema>(), findRequest(), options("surf-punish")).await()
            }
        } finally {
            client.close().await()
        }
    }

    @Test
    fun globalServiceCall() = e2eTest {
        val raw = rawNats()
        val client = TestNats.client(uniqueName("rpc-caller"))
        try {
            raw.subscribe("registry-service.\$rpc.v1.lookup", queue = "registry-service") { request ->
                raw.respond(request, raw.encode(FindPlayerResponseSchema, unsafeJso<FindPlayerResponse> { name = "global" }))
            }
            raw.flush()
            val response = client.call(method(RegistryService, "lookup"), findRequest("x")).await()
            assertEquals("global", response.fields<FindPlayerResponse>().name)
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun allInstancesFirstKnownAnswerWins() = e2eTest {
        val owner = uniqueName("rpc-owner")
        val raw = rawNats()
        val client = TestNats.client(uniqueName("rpc-caller"))
        try {
            val locate = subject(owner, "locate")
            repeat(2) {
                raw.subscribe(locate) { request ->
                    val headers = raw.jvmHeaders()
                    headers.set(ProtocolHeaders.ANSWER, ProtocolHeaders.ANSWER_UNKNOWN)
                    raw.respond(request, emptyBody(), headers)
                }
            }
            raw.subscribe(locate) { request ->
                raw.respond(request, raw.encode(LocateResponseSchema, unsafeJso<LocateResponse> { server = "lobby-2" }))
            }
            raw.flush()

            val started = epochMillis()
            val answer = client.callAnswer(method(PlayerService, "locate"), findRequest("Steve"), options(owner, 5_000)).await()
            assertEquals(true, answer.known)
            assertEquals("lobby-2", answer.value.fields<LocateResponse>().server)
            assertTrue(epochMillis() - started < 4_000, "the first known answer ends the call")
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun allInstancesUnknownErrorsAndNoResponders() = e2eTest {
        val owner = uniqueName("rpc-owner")
        val raw = rawNats()
        val client = TestNats.client(uniqueName("rpc-caller"))
        try {
            val locate = method(PlayerService, "locate")
            val request = findRequest("Steve")

            val started = epochMillis()
            val none = client.callAnswer(locate, request, options(owner, 5_000)).await()
            assertEquals(false, none.known)
            assertTrue(epochMillis() - started < 4_000, "no responders ends the call")

            val unknownSubscription = raw.subscribe(subject(owner, "locate")) { message ->
                val headers = raw.jvmHeaders()
                headers.set(ProtocolHeaders.ANSWER, ProtocolHeaders.ANSWER_UNKNOWN)
                raw.respond(message, emptyBody(), headers)
            }
            raw.flush()
            assertEquals(false, client.callAnswer(locate, request, options(owner, 400)).await().known)

            raw.subscribe(subject(owner, "locate")) { message ->
                val headers = raw.jvmHeaders()
                headers.set(ProtocolHeaders.ERROR, "java.lang.RuntimeException")
                raw.respond(message, utf8Bytes("broken"), headers)
            }
            raw.flush()
            val error = assertFails { client.callAnswer(locate, request, options(owner, 400)).await() }
            assertEquals("java.lang.RuntimeException", assertIs<NatsRemoteError>(error).type)
            unknownSubscription.unsubscribe()
        } finally {
            client.close().await()
            raw.close()
        }
    }

    @Test
    fun allInstancesWithoutResultFansOut() = e2eTest {
        val owner = uniqueName("rpc-owner")
        val raw = rawNats()
        val client = TestNats.client(uniqueName("rpc-caller"))
        try {
            val received = mutableListOf<Msg>()
            repeat(2) { raw.subscribe(subject(owner, "broadcast-all")) { received += it } }
            raw.flush()

            val result = client.call(method(PlayerService, "broadcast"), findRequest("all"), options(owner)).await()
            assertEquals("undefined", jsTypeOf(result))

            eventually { received.size == 2 }
            delay(100.milliseconds)
            assertEquals(2, received.size)
            assertTrue(received.all { it.reply == null || it.reply == "" }, "fan-out has no reply subject")
            assertTrue(received.all { header(it, ProtocolHeaders.DEADLINE) != null })
            assertEquals("all", raw.decode(FindPlayerRequestSchema, received.first().data).fields<FindPlayerRequest>().name)
        } finally {
            client.close().await()
            raw.close()
        }
    }
}
