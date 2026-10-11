package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsFormat
import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.service
import io.nats.client.Message
import io.nats.client.impl.Headers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceProxyTest {

    @Serializable
    data class Health(val value: Double)

    @Suppress("unused")
    @NatsService("calc")
    interface CalcService {
        suspend fun mixed(a: Int, b: Long, factor: Double, flag: Boolean, label: String): String
        suspend fun health(name: String): Health?
        suspend fun fire(message: String)
        suspend fun lookup(id: Long): Answer<String>

        @NatsTimeout(15)
        suspend fun slow(): String

        @NatsFormat(WireFormat.JSON)
        suspend fun jsonReport(playerName: String, sampleCount: Int): Health

        fun local(): String = "local"
    }

    private var clientCounter = 0

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @Test
    fun `arguments including primitives arrive and the result is decoded`() = withService { client, calc ->
        val method = client.method("mixed")
        val received = AtomicReference<Array<Any?>>()

        respond(client, "mixed") { request ->
            val contentType = request.headers?.getFirst(NatsHeaders.CONTENT_TYPE)
            received.set(client.codec.decode(method.arguments, request.data, contentType))
            reply(client, method, "ok", request)
        }.use {
            assertEquals("ok", calc.mixed(1, 2L, 3.5, true, "label"))
        }

        assertContentEquals(arrayOf(1, 2L, 3.5, true, "label"), received.get())
    }

    @Test
    fun `NatsFormat JSON method sends a JSON request and decodes a JSON reply`() = withService { client, calc ->
        val method = client.method("jsonReport")
        assertEquals(WireFormat.JSON, method.format)
        val contentType = AtomicReference<String?>()
        val body = AtomicReference<String>()

        respond(client, "jsonReport") { request ->
            contentType.set(request.headers?.getFirst(NatsHeaders.CONTENT_TYPE))
            body.set(request.data.decodeToString())
            reply(client, method, Health(17.5), request, WireFormat.JSON)
        }.use {
            assertEquals(Health(17.5), calc.jsonReport("steve", 3))
        }

        assertEquals(WireFormat.JSON.contentType, contentType.get())
        assertEquals("""{"player_name":"steve","sample_count":3}""", body.get())
    }

    @Test
    fun `request carries a deadline header`() = withService { client, calc ->
        val deadline = AtomicReference<Long>()
        val start = System.currentTimeMillis()

        respond(client, "health") { request ->
            deadline.set(request.headers.getFirst(NatsHeaders.DEADLINE)!!.toLong())
            reply(client, client.method("health"), null, request)
        }.use { calc.health("x") }

        val remaining = deadline.get() - start
        assertTrue(remaining in 4_000..6_000, "deadline should be about 5s ahead, was ${remaining}ms")
    }

    @Test
    fun `nullable result can be null`() = withService { client, calc ->
        respond(client, "health") { reply(client, client.method("health"), null, it) }.use {
            assertNull(calc.health("x"))
        }
    }

    @Test
    fun `nullable result with value`() = withService { client, calc ->
        respond(client, "health") { reply(client, client.method("health"), Health(17.5), it) }.use {
            assertEquals(Health(17.5), calc.health("x"))
        }
    }

    @Test
    fun `unit method returns after an empty reply`() = withService { client, calc ->
        respond(client, "fire") { request -> NatsConnectionManager.connection().publish(request.replyTo, ByteArray(0)) }.use {
            calc.fire("hello")
        }
    }

    @Test
    fun `answer of and none`() = withService { client, calc ->
        val method = client.method("lookup")

        respond(client, "lookup") { reply(client, method, "server-1", it) }.use {
            assertEquals(Answer.Of("server-1"), calc.lookup(1))
        }

        respond(client, "lookup") { request ->
            val headers = Headers().add(NatsHeaders.ANSWER, NatsHeaders.ANSWER_NONE)
            NatsConnectionManager.connection().publish(request.replyTo, headers, ByteArray(0))
        }.use {
            assertEquals(Answer.None, calc.lookup(1))
        }
    }

    @Test
    fun `remote error becomes NatsRemoteException`() = withService { client, calc ->
        respond(client, "health") { request ->
            val headers = Headers().add(NatsHeaders.ERROR, "java.lang.IllegalStateException")
            NatsConnectionManager.connection().publish(request.replyTo, headers, "boom".encodeToByteArray())
        }.use {
            val exception = assertFailsWith<NatsRemoteException> { calc.health("x") }
            assertEquals("java.lang.IllegalStateException", exception.remoteType)
            assertEquals("boom", exception.remoteMessage)
        }
    }

    @Test
    fun `missing responder fails immediately`() = withService { _, calc ->
        val start = TimeSource.Monotonic.markNow()
        assertFailsWith<NatsNoRespondersException> { calc.health("x") }
        assertTrue(start.elapsedNow() < 2.seconds, "took ${start.elapsedNow()}")
    }

    @Test
    fun `silent responder times out and withNatsTimeout overrides NatsTimeout`() = withService { client, calc ->
        respond(client, "slow") { /* never replies */ }.use {
            val start = TimeSource.Monotonic.markNow()
            val exception = assertFailsWith<NatsRequestTimeoutException> {
                withNatsCallTimeout(300.milliseconds) { calc.slow() }
            }
            assertEquals(300.milliseconds, exception.timeout)
            assertTrue(start.elapsedNow() < 1.seconds, "took ${start.elapsedNow()}")
        }
    }

    @Test
    fun `object methods and default methods work on the proxy`() = withService { client, calc ->
        assertNotNull(calc.toString())
        assertEquals("local", calc.local())
        assertSame(calc, client.service<CalcService>())
        assertEquals(CalcService::class.java.name + "\$NatsProxy", calc.javaClass.name)
        assertSame(CalcService::class.java.classLoader, calc.javaClass.classLoader)
    }

    @Test
    fun `repeated service calls return the same proxy instance`() = withService { client, calc ->
        assertSame(calc, client.service<CalcService>())
        assertSame(calc, client.service(CalcService::class))
    }

    @Test
    fun `proxy call after the client was closed fails`() = withService { client, calc ->
        client.close()

        val exception = assertFailsWith<IllegalStateException> { calc.health("x") }
        assertContains(exception.message.orEmpty(), "is closed")
        assertFailsWith<IllegalStateException> { client.service<CalcService>() }
    }

    @Test
    fun `proxy class is shared between clients`() = withService { _, calc ->
        (NatsClient.create("rpc-proxy-shared") as NatsClientImpl).use { other ->
            val otherCalc = other.service<CalcService>()
            assertNotSame(calc, otherCalc)
            assertSame(calc.javaClass, otherCalc.javaClass)
        }
    }

    private fun withService(block: suspend CoroutineScope.(NatsClientImpl, CalcService) -> Unit) = runTest {
        (NatsClient.create("rpc-proxy-${clientCounter++}") as NatsClientImpl).use { client ->
            withContext(Dispatchers.Default) { block(client, client.service<CalcService>()) }
        }
    }

    private fun NatsClientImpl.method(name: String) =
        serviceDescriptor(CalcService::class.java).methods.single { it.method.name == name }

    private fun respond(client: NatsClientImpl, methodName: String, handler: (Message) -> Unit): AutoCloseable {
        val connection = NatsConnectionManager.connection()
        val dispatcher = connection.createDispatcher()
        dispatcher.subscribe(client.method(methodName).subject) { handler(it) }
        connection.flush(Duration.ofSeconds(5))
        return AutoCloseable { connection.closeDispatcher(dispatcher) }
    }

    private fun reply(
        client: NatsClientImpl,
        method: MethodDescriptor,
        value: Any?,
        request: Message,
        format: WireFormat = WireFormat.CBOR,
    ) {
        val serializer = when (val result = method.result) {
            is ResultKind.Value -> result.serializer
            is ResultKind.Answer -> result.serializer
            ResultKind.Unit -> error("unit methods reply with an empty body")
        }
        val data = client.codec.encode(serializer, value, format)
        NatsConnectionManager.connection()
            .publish(request.replyTo, NatsHeaders.base(client.name, format), data)
    }
}
