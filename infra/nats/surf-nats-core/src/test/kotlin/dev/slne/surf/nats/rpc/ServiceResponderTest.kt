package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.impl.Headers
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.reflect.KClass
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceResponderTest {

    @Serializable
    data class Health(val value: Double)

    @NatsService("calc")
    interface CalcService {
        suspend fun mixed(a: Int, b: Long, factor: Double, flag: Boolean, label: String): String
        suspend fun health(name: String): Health?
        suspend fun fire(message: String)
        suspend fun lookup(id: Long): Answer<String>
        suspend fun fail(message: String): String
        suspend fun innerTimeout(): String
        suspend fun count(): Int

        @NatsTimeout(15)
        suspend fun slow(): String
    }

    class CalcImpl : CalcService {
        val fired = Channel<String>(Channel.UNLIMITED)
        val calls = AtomicInteger()
        val slowStarted = CompletableDeferred<Unit>()
        val slowCancelled = CompletableDeferred<Unit>()

        override suspend fun mixed(
            a: Int,
            b: Long,
            factor: Double,
            flag: Boolean,
            label: String
        ): String {
            return "$a|$b|$factor|$flag|$label"
        }

        override suspend fun health(name: String): Health? {
            return if (name == "none") null else Health(name.length.toDouble())
        }

        override suspend fun fire(message: String) {
            fired.send(message)
        }

        override suspend fun lookup(id: Long): Answer<String> {
            return if (id == 0L) Answer.None else Answer.Of("server-$id")
        }

        override suspend fun fail(message: String): String = throw IllegalStateException(message)

        override suspend fun innerTimeout(): String = withTimeout(10.milliseconds) {
            delay(5.seconds)
            "never"
        }

        override suspend fun count(): Int = calls.incrementAndGet()

        override suspend fun slow(): String {
            slowStarted.complete(Unit)
            try {
                awaitCancellation()
            } catch (e: CancellationException) {
                slowCancelled.complete(Unit)
                throw e
            }
        }
    }

    internal class Fixture(
        val client: NatsClientImpl,
        val calc: CalcService,
        val impl: CalcImpl,
        val registration: NatsRegistration,
    )

    private var clientCounter = 0

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @Test
    fun `primitive and string arguments roundtrip`() = withService {
        assertEquals("1|2|3.5|true|label", calc.mixed(1, 2L, 3.5, true, "label"))
    }

    @Test
    fun `nullable result roundtrips null and value`() = withService {
        assertNull(calc.health("none"))
        assertEquals(Health(3.0), calc.health("abc"))
    }

    @Test
    fun `unit method returns after the implementation ran`() = withService {
        calc.fire("hello")
        assertEquals("hello", impl.fired.tryReceive().getOrNull())
    }

    @Test
    fun `answer of and none roundtrip`() = withService {
        assertEquals(Answer.Of("server-1"), calc.lookup(1))
        assertEquals(Answer.None, calc.lookup(0))
    }

    @Test
    fun `implementation exception arrives as NatsRemoteException`() = withService {
        val exception = assertFailsWith<NatsRemoteException> { calc.fail("boom") }
        assertEquals(IllegalStateException::class.java.name, exception.remoteType)
        assertEquals("boom", exception.remoteMessage)
    }

    @Test
    fun `timeout inside the implementation is a remote error, not a caller timeout`() =
        withService {
            val exception = assertFailsWith<NatsRemoteException> { calc.innerTimeout() }
            assertEquals(TimeoutCancellationException::class.java.name, exception.remoteType)
        }

    @Test
    fun `closed registration has no responders`() = withService {
        assertEquals(1, calc.count())
        registration.close()

        assertFalse(registration.isActive)
        assertFailsWith<NatsNoRespondersException> { calc.count() }
    }

    @Test
    fun `service can be registered again after close`() = withService {
        registration.close()
        client.registerService<CalcService>(CalcImpl())
        assertEquals(1, calc.count())
    }

    @Test
    fun `withNatsTimeout overrides NatsTimeout and cancels the implementation`() = withService {
        val start = TimeSource.Monotonic.markNow()
        val exception = assertFailsWith<NatsRequestTimeoutException> {
            withNatsCallTimeout(300.milliseconds) { calc.slow() }
        }

        assertEquals(300.milliseconds, exception.timeout)
        assertTrue(start.elapsedNow() < 5.seconds, "took ${start.elapsedNow()}")
        assertTrue(impl.slowStarted.isCompleted)
        withTimeout(5.seconds) { impl.slowCancelled.await() }
    }

    @Test
    fun `queue group executes every request exactly once`() = withService {
        val method = method("count")
        val result = method.result as ResultKind.Value
        val otherCalls = AtomicInteger()

        val connection = NatsConnectionManager.connection()
        val dispatcher = connection.createDispatcher()
        dispatcher.subscribe(method.subject, client.name) { request ->
            val data = client.codec.encode(
                result.serializer,
                otherCalls.incrementAndGet(),
                WireFormat.CBOR
            )
            connection.publish(
                request.replyTo,
                NatsHeaders.base(client.name, WireFormat.CBOR),
                data
            )
        }
        connection.flush(Duration.ofSeconds(5))

        try {
            val count = 40
            repeat(count) { calc.count() }

            assertEquals(count, impl.calls.get() + otherCalls.get(), "every request exactly once")
            assertTrue(impl.calls.get() > 0, "registered service received no requests")
            assertTrue(otherCalls.get() > 0, "other queue member received no requests")
        } finally {
            connection.closeDispatcher(dispatcher)
        }
    }

    @Test
    fun `expired deadline is dropped without calling the implementation`() = withService {
        val method = method("count")
        val connection = NatsConnectionManager.connection()
        val inbox = connection.createInbox()
        val replies = connection.subscribe(inbox)
        connection.flush(Duration.ofSeconds(5))

        val headers = NatsHeaders.base("external", WireFormat.CBOR)
            .add(NatsHeaders.DEADLINE, (System.currentTimeMillis() - 1_000).toString())
        val body = client.codec.encode(method.arguments, arrayOf(), WireFormat.CBOR)
        connection.publish(method.subject, inbox, headers, body)

        assertNull(
            replies.nextMessage(Duration.ofMillis(500)),
            "expired request must not be answered"
        )
        assertEquals(0, impl.calls.get())
        replies.unsubscribe()
    }

    @Test
    fun `json request without deadline gets a json reply`() = withService {
        val headers = Headers().add(NatsHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
        val body = """{"a":1,"b":2,"factor":3.5,"flag":true,"label":"panel"}""".encodeToByteArray()

        val reply = NatsConnectionManager.connection()
            .request(method("mixed").subject, headers, body, Duration.ofSeconds(5))

        assertNotNull(reply, "no reply")
        assertNull(reply.headers.getFirst(NatsHeaders.ERROR), reply.data.decodeToString())
        val contentType = assertNotNull(reply.headers.getFirst(NatsHeaders.CONTENT_TYPE))
        assertEquals(WireFormat.JSON, WireFormat.fromContentType(contentType))
        assertEquals("\"1|2|3.5|true|panel\"", reply.data.decodeToString())
    }

    @Test
    fun `undecodable arguments are answered with an error`() = withService {
        val headers = Headers().add(NatsHeaders.CONTENT_TYPE, WireFormat.CBOR.contentType)

        val reply = NatsConnectionManager.connection()
            .request(
                method("mixed").subject,
                headers,
                byteArrayOf(0x7f, 0x01, 0x02),
                Duration.ofSeconds(5)
            )

        assertNotNull(reply, "no reply")
        val remoteType = assertNotNull(reply.headers.getFirst(NatsHeaders.ERROR))
        assertTrue(
            SerializationException::class.java.isAssignableFrom(Class.forName(remoteType)),
            "expected a SerializationException, was $remoteType"
        )
    }

    @Test
    fun `request without reply subject still runs the implementation`() = withService {
        val method = method("fire")
        val body = client.codec.encode(method.arguments, arrayOf<Any?>("fan-out"), WireFormat.CBOR)
        NatsConnectionManager.connection()
            .publish(method.subject, NatsHeaders.base("external", WireFormat.CBOR), body)

        assertEquals("fan-out", withTimeout(5.seconds) { impl.fired.receive() })
    }

    @Test
    fun `registering the same service twice fails`() = withService {
        assertFailsWith<IllegalStateException> { client.registerService<CalcService>(CalcImpl()) }
        assertEquals(1, calc.count(), "first registration must stay active")
    }

    @Test
    fun `implementation of the wrong type is rejected`() = withService {
        @Suppress("UNCHECKED_CAST")
        val type = CalcService::class as KClass<Any>
        assertFailsWith<IllegalArgumentException> { client.registerService(type, Any()) }
    }

    private fun withService(block: suspend Fixture.() -> Unit) = runTest {
        (NatsClient.create("rpc-responder-${clientCounter++}") as NatsClientImpl).use { client ->
            val impl = CalcImpl()
            val registration = client.registerService<CalcService>(impl)
            val fixture = Fixture(client, client.service(), impl, registration)
            withContext(Dispatchers.Default) { fixture.block() }
        }
    }

    private fun Fixture.method(name: String) =
        client.serviceDescriptor(CalcService::class.java).methods.single { it.method.name == name }
}
