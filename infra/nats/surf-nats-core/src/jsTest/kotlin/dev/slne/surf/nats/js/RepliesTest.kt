package dev.slne.surf.nats.js

import dev.slne.surf.nats.descriptor.NatsResultKind
import dev.slne.surf.nats.js.external.nats.core.Msg
import dev.slne.surf.nats.js.external.nats.core.NoRespondersError
import dev.slne.surf.nats.js.external.nats.core.RequestError
import dev.slne.surf.nats.js.external.nats.core.TimeoutError
import dev.slne.surf.nats.js.external.nats.core.headers
import dev.slne.surf.nats.js.external.protobuf.create
import dev.slne.surf.nats.js.external.protobuf.toBinary
import dev.slne.surf.nats.js.fixtures.FindPlayerResponse
import dev.slne.surf.nats.js.fixtures.FindPlayerResponseSchema
import dev.slne.surf.nats.js.fixtures.fields
import js.errors.ErrorOptions
import js.objects.unsafeJso
import js.typedarrays.Uint8Array
import kotlinx.coroutines.channels.Channel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RepliesTest {

    private val rawBody: suspend (Msg) -> Uint8Array<*> = { it.data }

    private fun decoder(kind: NatsResultKind) = ReplyDecoder(FindPlayerResponseSchema, kind)

    private suspend fun decode(kind: NatsResultKind, reply: Msg): ProtoMessage? = decoder(kind).decode(reply, rawBody)

    private fun responseMessage(name: String) = create(FindPlayerResponseSchema, unsafeJso<FindPlayerResponse> { this.name = name })

    private fun response(name: String) = toBinary(FindPlayerResponseSchema, responseMessage(name))

    private fun errorReply(type: String, message: String) =
        fakeMessage(utf8Bytes(message), protobufHeaders("Surf-Error" to type))

    private fun unknownReply() = fakeMessage(Wire.emptyBody(), protobufHeaders("Surf-Answer" to "unknown"))

    private fun cause(error: Throwable): ErrorOptions = unsafeJso { cause = error }

    @Test
    fun requestErrorsMapToSurfErrors() {
        val noResponders = RequestError("no responders", cause(NoRespondersError("a.b")))
        val mapped = assertIs<NatsNoRespondersError>(mapRequestError(noResponders, "a.b", 100))
        assertEquals("a.b", mapped.subject)

        val direct = assertIs<NatsNoRespondersError>(mapRequestError(NoRespondersError("c.d"), "c.d", 100))
        assertEquals("c.d", direct.subject)

        val timeout = assertIs<NatsCallTimeoutError>(mapRequestError(TimeoutError(), "a.b", 250))
        assertEquals("a.b", timeout.subject)
        assertEquals(250, timeout.timeoutMs)

        val wrappedTimeout = RequestError("timeout", cause(TimeoutError()))
        assertIs<NatsCallTimeoutError>(mapRequestError(wrappedTimeout, "a.b", 250))

        val other = IllegalStateException("other")
        assertSame(other, mapRequestError(other, "a.b", 250))
    }

    @Test
    fun surfErrorBecomesNatsRemoteError() = asyncTest {
        val error = assertFails { decode(NatsResultKind.VALUE, errorReply("java.lang.IllegalStateException", "boom")) }
        val remote = assertIs<NatsRemoteError>(error)
        assertEquals("java.lang.IllegalStateException", remote.type)
        assertEquals("boom", remote.remoteMessage)
        assertEquals("java.lang.IllegalStateException: boom", remote.message)
        assertIs<NatsRpcError>(remote)
    }

    @Test
    fun unitIgnoresTheBody() = asyncTest {
        val result = decode(NatsResultKind.UNIT, fakeMessage(bytes(1, 2, 3), protobufHeaders()))
        assertEquals("undefined", jsTypeOf(result))
    }

    @Test
    fun valueIsDecoded() = asyncTest {
        val result = decode(NatsResultKind.VALUE, fakeMessage(response("Steve"), protobufHeaders()))
        assertEquals("Steve", result.fields<FindPlayerResponse>().name)
    }

    @Test
    fun valueRequiresTheProtobufContentType() = asyncTest {
        val missing = assertFailsWith<IllegalArgumentException> { decode(NatsResultKind.VALUE, fakeMessage(response("x"), headers())) }
        assertTrue(missing.message!!.contains("application/protobuf"), missing.message)

        val json = headers()
        json.set("Content-Type", "application/json")
        assertFailsWith<IllegalArgumentException> {
            decode(NatsResultKind.VALUE, fakeMessage(response("x"), json))
        }

        val withParameters = headers()
        withParameters.set("Content-Type", "Application/Protobuf; charset=binary")
        val decoded = decode(NatsResultKind.VALUE, fakeMessage(response("x"), withParameters))
        assertEquals("x", decoded.fields<FindPlayerResponse>().name)
    }

    @Test
    fun answerRepliesAreCollectedByAllAnswers() = asyncTest {
        assertFailsWith<IllegalStateException> { decode(NatsResultKind.ANSWER, unknownReply()) }

        val surfError = assertFails { decode(NatsResultKind.ANSWER, errorReply("E", "x")) }
        assertEquals("E", assertIs<NatsRemoteError>(surfError).type)
    }

    private suspend fun collect(vararg replies: Msg): Answer {
        val channel = Channel<Msg>(Channel.UNLIMITED)
        replies.forEach { channel.trySend(it) }
        channel.close()
        return AllAnswers(decoder(NatsResultKind.ANSWER), rawBody, "a.b").collect(channel)
    }

    @Test
    fun answerKnownAndUnknown() = asyncTest {
        assertEquals(false, collect(unknownReply()).known)

        val known = collect(fakeMessage(response("Alex"), protobufHeaders()))
        assertEquals(true, known.known)
        assertEquals("Alex", known.value.fields<FindPlayerResponse>().name)
    }

    @Test
    fun allFirstKnownAnswerWins() = asyncTest {
        val answer = collect(unknownReply(), errorReply("E", "x"), fakeMessage(response("lobby"), protobufHeaders()))
        assertEquals(true, answer.known)
        assertEquals("lobby", answer.value.fields<FindPlayerResponse>().name)
    }

    @Test
    fun allWithoutAnswersIsUnknown() = asyncTest {
        assertEquals(false, collect(unknownReply(), unknownReply()).known)
        assertEquals(false, collect().known)
    }

    @Test
    fun allNoRespondersIsUnknown() = asyncTest {
        val status = fakeMessage(Wire.emptyBody(), headers(503, "No Responders"))
        assertEquals(false, collect(status, errorReply("E", "after")).known)
    }

    @Test
    fun allThrowsTheFirstRecordedError() = asyncTest {
        val error = assertFails { collect(unknownReply(), errorReply("First", "1"), errorReply("Second", "2")) }
        assertEquals("First", assertIs<NatsRemoteError>(error).type)

        val undecodable = assertFails {
            collect(fakeMessage(bytes(0x0a, 0x7f), protobufHeaders()), unknownReply())
        }
        assertTrue(undecodable !is NatsRemoteError)
    }

    @Test
    fun answerObjectsHaveTheTsShape() {
        val message = responseMessage("v")
        val known = knownAnswer(message)
        assertEquals(true, known.known)
        assertSame(message, known.value)
        assertEquals(listOf("known", "value"), objectKeys(known))
        val unknown = unknownAnswer()
        assertEquals(false, unknown.known)
        assertEquals(listOf("known"), objectKeys(unknown))
    }

    private fun objectKeys(value: Any): List<String> = JsObject.keys(value).toList()
}
