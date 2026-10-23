package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.fixtures.Tag
import dev.slne.surf.nats.test.proto.v1.CalcService
import dev.slne.surf.nats.test.proto.v1.Health
import dev.slne.surf.nats.test.proto.v1.HealthReply
import dev.slne.surf.nats.test.proto.v1.InlineRequest
import dev.slne.surf.nats.test.proto.v1.MixedRequest
import dev.slne.surf.nats.test.proto.v1.Text
import dev.slne.surf.nats.test.proto.v1.UnsignedReply
import dev.slne.surf.nats.test.proto.v1.UnsignedRequest
import io.nats.client.Message
import io.nats.client.impl.Headers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceProxyTest {

    private var clientCounter = 0

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @Test
    fun `arguments including primitives arrive and the result is decoded`() = withService { client, calc ->
        val method = client.method("mixed")
        val received = AtomicReference<Any>()

        respond(client, "mixed") { request ->
            received.set(decodeRequest(method, request))
            reply(client, method, Text("ok"), request)
        }.use {
            assertEquals(Text("ok"), calc.mixed(1, 2L, 3.5, true, "label"))
        }

        assertEquals(MixedRequest(1, 2L, 3.5, true, "label"), received.get())
    }

    @Test
    fun `value class arguments are boxed into the request message`() = withService { client, calc ->
        val method = client.method("inline")
        val received = LinkedBlockingQueue<Any>()

        respond(client, "inline") { request ->
            received.add(decodeRequest(method, request))
            reply(client, method, Text("ok"), request)
        }.use {
            calc.inline(1.5.seconds, null, Tag("a"), Tag("b"))
            calc.inline(2.seconds, 3.seconds, Tag("c"), null)
        }

        assertEquals(InlineRequest(1.5.seconds, null, Tag("a"), Tag("b")), received.poll(5, TimeUnit.SECONDS))
        assertEquals(InlineRequest(2.seconds, 3.seconds, Tag("c"), null), received.poll(5, TimeUnit.SECONDS))
    }

    @Test
    fun `omitted default arguments are filled in by the caller`() = withService { client, calc ->
        val unsigned = client.method("unsigned")
        val inline = client.method("inline")
        val received = LinkedBlockingQueue<Any>()

        respond(client, "unsigned") { request ->
            received.add(decodeRequest(unsigned, request))
            reply(client, unsigned, UnsignedReply(1, 2), request)
        }.use {
            respond(client, "inline") { request ->
                received.add(decodeRequest(inline, request))
                reply(client, inline, Text("ok"), request)
            }.use {
                assertEquals(UnsignedReply(1, 2), calc.unsigned(1, 2))
                assertEquals(Text("ok"), calc.inline(1.seconds, tag = Tag("t")))
            }
        }

        assertEquals(UnsignedRequest(1, 2, null), received.poll(5, TimeUnit.SECONDS))
        assertEquals(InlineRequest(1.seconds, null, Tag("t"), null), received.poll(5, TimeUnit.SECONDS))
    }

    @Test
    fun `request carries a deadline header`() = withService { client, calc ->
        val deadline = AtomicReference<Long>()
        val start = System.currentTimeMillis()

        respond(client, "health") { request ->
            deadline.set(request.headers.getFirst(ProtocolHeaders.DEADLINE)!!.toLong())
            reply(client, client.method("health"), HealthReply(), request)
        }.use { calc.health() }

        val remaining = deadline.get() - start
        assertTrue(remaining in 4_000..6_000, "deadline should be about 5s ahead, was ${remaining}ms")
    }

    @Test
    fun `response without fields is decoded from an empty body`() = withService { client, calc ->
        val method = client.method("health")
        assertEquals(0, NatsCodec.encode(checkNotNull(method.response), HealthReply()).size)

        respond(client, "health") { reply(client, method, HealthReply(), it) }.use {
            assertEquals(HealthReply(null), calc.health())
        }
    }

    @Test
    fun `response with a nullable field set`() = withService { client, calc ->
        respond(client, "health") { reply(client, client.method("health"), HealthReply(Health(17.5)), it) }.use {
            assertEquals(HealthReply(Health(17.5)), calc.health())
        }
    }

    @Test
    fun `unit method returns after an empty reply`() = withService { client, calc ->
        respond(client, "fire") { request -> NatsConnectionManager.connection().publish(request.replyTo, ByteArray(0)) }.use {
            calc.fire()
        }
    }

    @Test
    fun `remote error becomes NatsRemoteException`() = withService { client, calc ->
        respond(client, "health") { request ->
            val headers = Headers().add(ProtocolHeaders.ERROR, "java.lang.IllegalStateException")
            NatsConnectionManager.connection().publish(request.replyTo, headers, "boom".encodeToByteArray())
        }.use {
            val exception = assertFailsWith<NatsRemoteException> { calc.health() }
            assertEquals("java.lang.IllegalStateException", exception.remoteType)
            assertEquals("boom", exception.remoteMessage)
        }
    }

    @Test
    fun `missing responder fails immediately`() = withService { _, calc ->
        val start = TimeSource.Monotonic.markNow()
        assertFailsWith<NatsNoRespondersException> { calc.health() }
        assertTrue(start.elapsedNow() < 2.seconds, "took ${start.elapsedNow()}")
    }

    @Test
    fun `silent responder times out and withNatsCallTimeout overrides the contract timeout`() = withService { client, calc ->
        assertEquals(15.seconds, client.method("slow").timeout)

        respond(client, "slow") { /* never replies */ }.use {
            val start = TimeSource.Monotonic.markNow()
            val exception = assertFailsWith<NatsCallTimeoutException> {
                withNatsCallTimeout(300.milliseconds) { calc.slow() }
            }
            assertEquals(300.milliseconds, exception.timeout)
            assertTrue(start.elapsedNow() < 1.seconds, "took ${start.elapsedNow()}")
        }
    }

    @Test
    fun `object methods work on the proxy`() = withService { client, calc ->
        assertNotNull(calc.toString())
        assertEquals(calc, calc)
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

        val exception = assertFailsWith<IllegalStateException> { calc.health() }
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
        serviceDescriptor(CalcService::class.java).methods.single { it.kotlinName == name }

    private fun respond(client: NatsClientImpl, methodName: String, handler: (Message) -> Unit): AutoCloseable {
        val connection = NatsConnectionManager.connection()
        val dispatcher = connection.createDispatcher()
        dispatcher.subscribe(client.method(methodName).subject) { handler(it) }
        connection.flush(Duration.ofSeconds(5))
        return AutoCloseable { connection.closeDispatcher(dispatcher) }
    }

    private fun decodeRequest(method: MethodDescriptor, request: Message): Any {
        return NatsCodec.decode(method.request, request.data, request.headers?.getFirst(ProtocolHeaders.CONTENT_TYPE))
    }

    private fun reply(
        client: NatsClientImpl,
        method: MethodDescriptor,
        value: Any,
        request: Message,
    ) {
        val data = NatsCodec.encode(checkNotNull(method.response), value)
        NatsConnectionManager.connection()
            .publish(request.replyTo, NatsHeaders.base(client.name), data)
    }
}
