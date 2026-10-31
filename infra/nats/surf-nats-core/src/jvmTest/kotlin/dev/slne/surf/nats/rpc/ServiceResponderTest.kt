package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.test.fixtures.Tag
import dev.slne.surf.nats.test.proto.v1.CalcService
import dev.slne.surf.nats.test.proto.v1.Color
import dev.slne.surf.nats.test.proto.v1.Count
import dev.slne.surf.nats.test.proto.v1.Created
import dev.slne.surf.nats.test.proto.v1.Everything
import dev.slne.surf.nats.test.proto.v1.FinderService
import dev.slne.surf.nats.test.proto.v1.Health
import dev.slne.surf.nats.test.proto.v1.HealthReply
import dev.slne.surf.nats.test.proto.v1.Text
import dev.slne.surf.nats.test.proto.v1.TypesService
import dev.slne.surf.nats.test.proto.v1.UnsignedReply
import io.nats.client.impl.Headers
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import dev.slne.surf.nats.test.proto.v1.VersionedPingReply as VersionedPingReplyV1
import dev.slne.surf.nats.test.proto.v1.VersionedService as VersionedServiceV1
import dev.slne.surf.nats.test.proto.v2.VersionedPingReply as VersionedPingReplyV2
import dev.slne.surf.nats.test.proto.v2.VersionedService as VersionedServiceV2

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceResponderTest {

    class CalcImpl : CalcService {
        val fired = Channel<Unit>(Channel.UNLIMITED)
        val calls = AtomicInteger()
        val slowStarted = CompletableDeferred<Unit>()
        val slowCancelled = CompletableDeferred<Unit>()
        val slowFireDone = CompletableDeferred<Unit>()
        val gatedStarted = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()

        @Volatile
        var health: Health? = null

        // Runs in gated() after the gate opens and supplies its result
        @Volatile
        var afterGate: (suspend () -> Text)? = null

        override suspend fun mixed(a: Int, b: Long, factor: Double, flag: Boolean, label: String): Text {
            return Text("$a|$b|$factor|$flag|$label")
        }

        override suspend fun health(): HealthReply = HealthReply(health)

        override suspend fun fire() {
            fired.send(Unit)
        }

        override suspend fun fail(message: String): Text = throw IllegalStateException(message)

        override suspend fun innerTimeout(): Text = withTimeout(10.milliseconds) {
            delay(5.seconds)
            Text("never")
        }

        override suspend fun count(): Count = Count(calls.incrementAndGet())

        override suspend fun slowFire() {
            delay(2.seconds)
            slowFireDone.complete(Unit)
        }

        override suspend fun gated(): Text {
            gatedStarted.complete(Unit)
            gate.await()
            return afterGate?.invoke() ?: Text("released")
        }

        override suspend fun unsigned(id: Long, small: Int, optionalId: Long?): UnsignedReply {
            return UnsignedReply(id, small, optionalId)
        }

        override suspend fun `inline`(duration: Duration, maybeDuration: Duration?, tag: Tag, maybeTag: Tag?): Text {
            return Text("$duration|$maybeDuration|${tag.value}|${maybeTag?.value}")
        }

        override suspend fun slow(): Text {
            slowStarted.complete(Unit)
            try {
                awaitCancellation()
            } catch (e: CancellationException) {
                slowCancelled.complete(Unit)
                throw e
            }
        }

        override suspend fun missing(): Text = TODO("not implemented yet")
    }

    class TypesImpl : TypesService {
        override suspend fun roundtrip(
            small: Int,
            large: Long,
            unsignedSmall: Int,
            unsignedLarge: Long,
            signedLarge: Long,
            fixedSmall: Int,
            fixedLarge: Long,
            signedFixedSmall: Int,
            signedFixedLarge: Long,
            flag: Boolean,
            ratio: Double,
            scale: Float,
            label: String,
            payload: ByteArray,
            names: List<String>,
            counts: Map<String, Int>,
            nested: Everything.Nested,
            color: Color,
            choice: Everything.Choice?,
            createdAt: Instant,
            duration: Duration,
            uuid: UUID,
            component: Component,
            key: Key,
            nullableLabel: String?,
            defaulted: Int,
            tag: Tag,
            nullableTag: Tag?,
            nestedList: List<Everything.Nested>,
            nestedById: Map<Long, Everything.Nested>,
        ) = Everything(
            small = small,
            large = large,
            unsignedSmall = unsignedSmall,
            unsignedLarge = unsignedLarge,
            signedLarge = signedLarge,
            fixedSmall = fixedSmall,
            fixedLarge = fixedLarge,
            signedFixedSmall = signedFixedSmall,
            signedFixedLarge = signedFixedLarge,
            flag = flag,
            ratio = ratio,
            scale = scale,
            label = label,
            payload = payload,
            names = names,
            counts = counts,
            nested = nested,
            color = color,
            choice = choice,
            createdAt = createdAt,
            duration = duration,
            uuid = uuid,
            component = component,
            key = key,
            nullableLabel = nullableLabel,
            defaulted = defaulted,
            tag = tag,
            nullableTag = nullableTag,
            nestedList = nestedList,
            nestedById = nestedById,
        )
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
        assertEquals(Text("1|2|3.5|true|label"), calc.mixed(1, 2L, 3.5, true, "label"))
    }

    @Test
    fun `unsigned values roundtrip as two's complement`() = withService {
        assertEquals(UnsignedReply(-1L, -1, Long.MIN_VALUE), calc.unsigned(-1L, -1, Long.MIN_VALUE))
        assertEquals(UnsignedReply(7, 0, null), calc.unsigned(7, 0, null))
    }

    @Test
    fun `value class arguments roundtrip`() = withService {
        assertTrue(Duration::class.java.isAnnotationPresent(JvmInline::class.java))
        assertTrue(Tag::class.java.isAnnotationPresent(JvmInline::class.java))

        assertEquals(Text("1.5s|null|a|b"), calc.inline(1.5.seconds, null, Tag("a"), Tag("b")))
        assertEquals(Text("2s|3s|a|null"), calc.inline(2.seconds, 3.seconds, Tag("a"), null))
        assertEquals(Text("-1ms|0s||"), calc.inline((-1).milliseconds, Duration.ZERO, Tag(""), Tag("")))
    }

    @Test
    fun `omitted default arguments roundtrip`() = withService {
        assertEquals(UnsignedReply(1, 2, null), calc.unsigned(1, 2))
        assertEquals(Text("1s|null|t|null"), calc.inline(1.seconds, tag = Tag("t")))
    }

    @Test
    fun `every field kind roundtrips through a service call`() = withService {
        client.registerService<TypesService>(TypesImpl())
        val types = client.service<TypesService>()
        val e = everything()

        val result = types.roundtrip(
            e.small, e.large, e.unsignedSmall, e.unsignedLarge, e.signedLarge, e.fixedSmall, e.fixedLarge,
            e.signedFixedSmall, e.signedFixedLarge, e.flag, e.ratio, e.scale, e.label, e.payload, e.names, e.counts,
            e.nested, e.color, e.choice, e.createdAt, e.duration, e.uuid, e.component, e.key, e.nullableLabel,
            e.defaulted, e.tag, e.nullableTag, e.nestedList, e.nestedById,
        )

        assertEquals(e, result)
    }

    @Test
    fun `response with and without its nullable field roundtrips`() = withService {
        assertEquals(HealthReply(null), calc.health())
        impl.health = Health(3.0)
        assertEquals(HealthReply(Health(3.0)), calc.health())
    }

    @Test
    fun `unit method returns after the implementation ran`() = withService {
        calc.fire()
        assertTrue(impl.fired.tryReceive().isSuccess)
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
        assertEquals(Count(1), calc.count())
        registration.close()

        assertTrue(registration.isClosed)
        assertFailsWith<NatsNoRespondersException> { calc.count() }
    }

    @Test
    fun `close answers requests that are already being handled`() = withService {
        coroutineScope {
            val call = async { calc.gated() }
            withTimeout(5.seconds) { impl.gatedStarted.await() }

            val closing = async(Dispatchers.IO) { registration.close() }
            delay(300.milliseconds)
            assertFalse(closing.isCompleted, "close returned while a request was still being handled")

            impl.gate.complete(Unit)
            assertEquals(Text("released"), call.await())
            withTimeout(5.seconds) { closing.await() }
        }
        assertFailsWith<NatsNoRespondersException> { calc.count() }
    }

    @Test
    fun `handlers running while the client closes can still publish and call through it`() = withService {
        NatsClient.create("rpc-responder-${clientCounter++}").use { other ->
            other.registerService<PingService>(object : PingService {
                override suspend fun ping() = Text("nested")
            })
            assertEquals(Text("nested"), client.service<PingService>().ping())

            val connection = NatsConnectionManager.connection()
            val events = connection.subscribe(client.eventDescriptor(Created::class.java).subject)
            connection.flush(java.time.Duration.ofSeconds(5))

            impl.afterGate = {
                client.publish(Created(7))
                client.service<PingService>().ping()
            }

            coroutineScope {
                val call = async { calc.gated() }
                withTimeout(5.seconds) { impl.gatedStarted.await() }

                val closing = async(Dispatchers.IO) { client.close() }
                delay(300.milliseconds)
                assertFalse(closing.isCompleted, "close returned while a request was still being handled")

                impl.gate.complete(Unit)
                assertEquals(Text("nested"), call.await())
                withTimeout(5.seconds) { closing.await() }
            }

            val event = assertNotNull(events.nextMessage(java.time.Duration.ofSeconds(5)), "event published during close")
            assertEquals(
                Created(7),
                NatsCodec.decode(Created.serializer(), event.data, event.headers.getFirst(ProtocolHeaders.CONTENT_TYPE)),
            )
            events.unsubscribe()

            assertTrue(client.isClosed)
            assertFailsWith<IllegalStateException> { client.publish(Created(8)) }
        }
    }

    @Test
    fun `closing client rejects new registrations and proxies but keeps existing proxies`() = withService {
        val versioned = object : VersionedServiceV1 {
            override suspend fun ping(message: String) = VersionedPingReplyV1(message, version = 1)
        }

        coroutineScope {
            val call = async { calc.gated() }
            withTimeout(5.seconds) { impl.gatedStarted.await() }

            val closing = async(Dispatchers.IO) { client.close() }
            delay(300.milliseconds)
            assertFalse(closing.isCompleted, "close returned while a request was still being handled")

            assertFalse(client.isClosed, "client counts as closed while it drains")
            assertSame(calc, client.service<CalcService>(), "existing proxies stay available")
            val proxy = assertFailsWith<IllegalStateException> { client.service<FinderService>() }
            assertContains(proxy.message.orEmpty(), "is closing")
            assertFailsWith<IllegalStateException> { client.registerListener(Any()) }
            assertFailsWith<IllegalStateException> { client.registerService<VersionedServiceV1>(versioned) }

            impl.gate.complete(Unit)
            assertEquals(Text("released"), call.await())
            withTimeout(5.seconds) { closing.await() }
        }

        assertTrue(client.isClosed)
        val closed = assertFailsWith<IllegalStateException> { client.service<CalcService>() }
        assertContains(closed.message.orEmpty(), "is closed")
    }

    @Test
    fun `service can be registered again after close`() = withService {
        registration.close()
        client.registerService<CalcService>(CalcImpl())
        assertEquals(Count(1), calc.count())
    }

    @Test
    fun `withNatsCallTimeout overrides the contract timeout and cancels the implementation`() = withService {
        warmUp()
        val start = TimeSource.Monotonic.markNow()
        val exception = assertFailsWith<NatsCallTimeoutException> {
            withNatsCallTimeout(300.milliseconds) { calc.slow() }
        }

        assertEquals(300.milliseconds, exception.timeout)
        assertTrue(start.elapsedNow() < 1.seconds, "took ${start.elapsedNow()}")
        assertTrue(impl.slowStarted.isCompleted)
        withTimeout(5.seconds) { impl.slowCancelled.await() }
    }

    @Test
    fun `outer caller timeout is not translated into NatsCallTimeoutException`() = withService {
        warmUp()
        val start = TimeSource.Monotonic.markNow()
        val result = withTimeoutOrNull(300.milliseconds) { calc.slow() }

        assertNull(result)
        assertTrue(start.elapsedNow() < 5.seconds, "took ${start.elapsedNow()}")
        assertTrue(impl.slowStarted.isCompleted)
    }

    @Test
    fun `queue group executes every request exactly once`() = withService {
        val method = method("count")
        val otherCalls = AtomicInteger()

        val connection = NatsConnectionManager.connection()
        val dispatcher = connection.createDispatcher()
        dispatcher.subscribe(method.subject, client.serviceDescriptor(CalcService::class.java).names.queueGroup) { request ->
            val data = NatsCodec.encode(checkNotNull(method.response), Count(otherCalls.incrementAndGet()))
            connection.publish(
                request.replyTo,
                NatsHeaders.base(client.name),
                data
            )
        }
        connection.flush(java.time.Duration.ofSeconds(5))

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
        connection.flush(java.time.Duration.ofSeconds(5))

        val headers = NatsHeaders.base("external")
            .add(ProtocolHeaders.DEADLINE, (System.currentTimeMillis() - 1_000).toString())
        connection.publish(method.subject, inbox, headers, method.encodeRequest(arrayOf()))

        assertNull(
            replies.nextMessage(java.time.Duration.ofMillis(500)),
            "expired request must not be answered"
        )
        assertEquals(0, impl.calls.get())
        replies.unsubscribe()
    }

    @Test
    fun `external protobuf request without deadline is answered in protobuf`() = withService {
        val method = method("mixed")
        val body = method.encodeRequest(arrayOf(1, 2L, 3.5, true, "panel"))

        val reply = requestReply(method.subject, NatsHeaders.base("external"), body)

        assertTrue(ProtocolHeaders.isProtobufContentType(reply.headers.getFirst(ProtocolHeaders.CONTENT_TYPE)))
        assertEquals(
            Text("1|2|3.5|true|panel"),
            NatsCodec.decode(checkNotNull(method.response), reply.data, reply.headers.getFirst(ProtocolHeaders.CONTENT_TYPE))
        )
    }

    @Test
    fun `undecodable arguments are answered with an error`() = withService {
        val headers = Headers().add(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)

        val reply = requestReply(method("mixed").subject, headers, byteArrayOf(0x7f, 0x01, 0x02))

        val remoteType = assertNotNull(reply.headers.getFirst(ProtocolHeaders.ERROR))
        assertTrue(
            SerializationException::class.java.isAssignableFrom(Class.forName(remoteType)),
            "expected a SerializationException, was $remoteType"
        )
    }

    @Test
    fun `request missing a non-null field is answered with an error`() = withService {
        val headers = Headers().add(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)

        val reply = requestReply(method("mixed").subject, headers, NatsHeaders.EMPTY_BODY)

        assertEquals(MissingFieldException::class.java.name, reply.headers.getFirst(ProtocolHeaders.ERROR))
        assertContains(reply.data.decodeToString(), "label")
    }

    @Test
    fun `truncated arguments are answered with a serialization error`() = withService {
        val headers = Headers().add(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)

        // FailRequest.message is field 1: length-delimited, announces 5 bytes that do not follow
        val reply = requestReply(method("fail").subject, headers, byteArrayOf(0x0a, 0x05))

        val remoteType = assertNotNull(reply.headers.getFirst(ProtocolHeaders.ERROR))
        assertTrue(
            SerializationException::class.java.isAssignableFrom(Class.forName(remoteType)),
            "expected a SerializationException, was $remoteType"
        )
    }

    @Test
    fun `request without reply subject still runs the implementation`() = withService {
        val method = method("fire")
        NatsConnectionManager.connection()
            .publish(method.subject, NatsHeaders.base("external"), method.encodeRequest(arrayOf()))

        withTimeout(5.seconds) { impl.fired.receive() }
    }

    @Test
    fun `fan-out handler runs past the caller deadline`() = withService {
        val method = method("slowFire")
        val headers = NatsHeaders.base("external")
            .add(ProtocolHeaders.DEADLINE, (System.currentTimeMillis() + 1_000).toString())
        NatsConnectionManager.connection().publish(method.subject, headers, method.encodeRequest(arrayOf()))

        withTimeout(5.seconds) { impl.slowFireDone.await() }
    }

    @Test
    fun `expired fan-out is dropped without calling the implementation`() = withService {
        val method = method("count")
        val headers = NatsHeaders.base("external")
            .add(ProtocolHeaders.DEADLINE, (System.currentTimeMillis() - 1_000).toString())
        NatsConnectionManager.connection().publish(method.subject, headers, method.encodeRequest(arrayOf()))

        delay(500.milliseconds)
        assertEquals(0, impl.calls.get())
    }

    @Test
    fun `registering the same service twice fails`() = withService {
        assertFailsWith<IllegalStateException> { client.registerService<CalcService>(CalcImpl()) }
        assertEquals(Count(1), calc.count(), "first registration must stay active")
    }

    @Test
    fun `different interfaces with the same service name cannot share a client`() = withService {
        val first = client.registerService<ClashServiceA>(object : ClashServiceA {
            override suspend fun ping() = Text("first")
        })

        val message = assertFailsWith<IllegalStateException> {
            client.registerService<ClashServiceB>(object : ClashServiceB {
                override suspend fun ping() = Text("second")
            })
        }.message.orEmpty()

        assertContains(message, "Service surf.nats.test.v1.NameClashService of ${ClashServiceB::class.java.name}")
        assertContains(message, "is already registered on NatsClient[")
        assertContains(message, ClashServiceA::class.java.name)
        assertFalse(first.isClosed)
        assertEquals(Text("first"), client.service<ClashServiceA>().ping(), "first registration must stay active")
    }

    @Test
    fun `v1 and v2 of one service are served side by side in one client`() = withService {
        val attempts = Channel<Int>(Channel.UNLIMITED)
        client.registerService<VersionedServiceV1>(object : VersionedServiceV1 {
            override suspend fun ping(message: String) = VersionedPingReplyV1(message, version = 1)
        })
        client.registerService<VersionedServiceV2>(object : VersionedServiceV2 {
            override suspend fun ping(message: String, attempt: Int): VersionedPingReplyV2 {
                attempts.send(attempt)
                return VersionedPingReplyV2(message, version = 2, region = "eu")
            }
        })

        assertEquals(VersionedPingReplyV1("a", 1), client.service<VersionedServiceV1>().ping("a"))
        assertEquals(VersionedPingReplyV2("b", 2, "eu"), client.service<VersionedServiceV2>().ping("b"))
        assertEquals(VersionedPingReplyV2("c", 2, "eu"), client.service<VersionedServiceV2>().ping("c", attempt = 3))
        assertEquals(listOf(1, 3), listOf(attempts.receive(), attempts.receive()))

        val v1 = client.serviceDescriptor(VersionedServiceV1::class.java).methods.single()
        val v2 = client.serviceDescriptor(VersionedServiceV2::class.java).methods.single()
        assertEquals("surf.rpc.surf.nats.test.v1.VersionedService.Ping", v1.subject)
        assertEquals("surf.rpc.surf.nats.test.v2.VersionedService.Ping", v2.subject)
    }

    @Test
    fun `implementation throwing an Error answers with the error instead of timing out`() = withService {
        val start = TimeSource.Monotonic.markNow()
        val exception = assertFailsWith<NatsRemoteException> { calc.missing() }

        assertEquals("kotlin.NotImplementedError", exception.remoteType)
        assertTrue(start.elapsedNow() < 3.seconds, "took ${start.elapsedNow()}")
    }

    @Test
    fun `implementation of the wrong type is rejected`() = withService {
        @Suppress("UNCHECKED_CAST")
        val type = (CalcService::class.java as Class<Any>).kotlin
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

    private suspend fun Fixture.warmUp() {
        assertEquals(Count(1), calc.count())
        impl.calls.set(0)
    }

    private fun Fixture.method(name: String) =
        client.serviceDescriptor(CalcService::class.java).methods.single { it.kotlinName == name }
}
