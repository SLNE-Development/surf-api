@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.jetstream.JetStreamProvisioner
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.runtime.RuntimeDefaults
import dev.slne.surf.nats.test.proto.v1.Blob
import dev.slne.surf.nats.test.proto.v1.Color
import dev.slne.surf.nats.test.proto.v1.ColorHolder
import dev.slne.surf.nats.test.proto.v1.LookupReply
import dev.slne.surf.nats.test.proto.v1.Number
import dev.slne.surf.nats.test.proto.v1.OrderService
import dev.slne.surf.nats.test.proto.v1.Text
import dev.slne.surf.nats.test.proto.v1.TotalRequest
import dev.slne.surf.nats.test.proto.v1.WideColor
import dev.slne.surf.nats.test.proto.v1.WideColorHolder
import io.nats.client.Message
import io.nats.client.api.StreamConfiguration
import io.nats.client.impl.Headers
import io.nats.client.support.NatsJetStreamConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import dev.slne.surf.nats.test.proto.v1.VersionedJournalService as VersionedJournalServiceV1
import dev.slne.surf.nats.test.proto.v1.VersionedPingReply as VersionedPingReplyV1
import dev.slne.surf.nats.test.proto.v2.VersionedJournalService as VersionedJournalServiceV2
import dev.slne.surf.nats.test.proto.v2.VersionedPingReply as VersionedPingReplyV2

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NatsPersistentServiceTest {

    class Delivery(val id: Long, val context: NatsPersistentContext?)

    class OrderImpl : OrderService {
        val deliveries = Channel<Delivery>(Channel.UNLIMITED)
        val bigReceived = Channel<ByteArray>(Channel.UNLIMITED)
        val painted = Channel<ColorHolder>(Channel.UNLIMITED)
        val totalCalls = AtomicInteger()
        val totalContexts = Channel<NatsPersistentContext?>(Channel.UNLIMITED)

        @Volatile
        var onPlace: suspend (Long, NatsPersistentContext?) -> Unit = { _, _ -> }

        @Volatile
        var onTotal: suspend (Long) -> Long = { it * 10 }

        override suspend fun place(id: Long) {
            val context = natsPersistentContextOrNull()
            deliveries.send(Delivery(id, context))
            onPlace(id, context)
        }

        override suspend fun total(id: Long): Number {
            totalCalls.incrementAndGet()
            totalContexts.send(natsPersistentContextOrNull())
            return Number(onTotal(id))
        }

        override suspend fun lookup(id: Long): LookupReply = LookupReply(if (id == 0L) null else "found-$id")

        override suspend fun big(data: ByteArray) {
            bigReceived.send(data)
        }

        override suspend fun echo(data: ByteArray): Blob = Blob(data)

        override suspend fun audit(id: Long): Number = Number(id)

        override suspend fun paint(
            color: Color,
            maybeColor: Color?,
            colors: List<Color>,
            colorsByName: Map<String, Color>,
        ) {
            painted.send(ColorHolder(color, maybeColor, colors, colorsByName))
        }

        override suspend fun ping(): Text = Text("pong")
    }

    class JournalV1 : VersionedJournalServiceV1 {
        val recorded = Channel<String>(Channel.UNLIMITED)

        override suspend fun record(message: String) {
            recorded.send(message)
        }

        override suspend fun append(message: String) = VersionedPingReplyV1(message, version = 1)
    }

    class JournalV2 : VersionedJournalServiceV2 {
        val recorded = Channel<Pair<String, Int>>(Channel.UNLIMITED)

        override suspend fun record(message: String, attempt: Int) {
            recorded.send(message to attempt)
        }

        override suspend fun append(message: String, attempt: Int) =
            VersionedPingReplyV2(message, version = 2, region = "attempt-$attempt")
    }

    private var clientCounter = 0
    private val defaultAckWait = RpcDefaults.ACK_WAIT
    private val defaultBackoff = RpcDefaults.NAK_BACKOFF
    private val defaultFireMaxDuration = RpcDefaults.FIRE_MAX_DURATION
    private val defaultDuplicateWindow = RuntimeDefaults.dedupeWindow

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @AfterEach
    fun resetDefaults() {
        RpcDefaults.ACK_WAIT = defaultAckWait
        RpcDefaults.NAK_BACKOFF = defaultBackoff
        RpcDefaults.FIRE_MAX_DURATION = defaultFireMaxDuration
        RuntimeDefaults.dedupeWindow = defaultDuplicateWindow
    }

    // ---- fire ----

    @Test
    fun `fire while the responder is offline is processed after it registers`() =
        withClient { client ->
            val streams = client.serviceDescriptor(OrderService::class.java).streams!!
            client.service<OrderService>().place(1)
            assertEquals(1, messageCount(streams.stream), "message must wait in the stream")

            val impl = OrderImpl()
            client.registerService<OrderService>(impl)

            val delivery = withTimeout(10.seconds) { impl.deliveries.receive() }
            assertEquals(1, delivery.id)
            val context = assertNotNull(delivery.context, "NatsPersistentContext missing")
            assertEquals(1, context.deliveryCount)
            assertTrue(context.messageId.isNotBlank())

            eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
        }

    @Test
    fun `fire is executed once and core methods are not stored`() = withClient { client ->
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val impl = OrderImpl()
        client.registerService<OrderService>(impl)
        val orders = client.service<OrderService>()

        assertEquals(Text("pong"), orders.ping())
        assertEquals(0, messageCount(streams.stream), "core call must not be stored")

        orders.place(7)
        assertEquals(7, withTimeout(10.seconds) { impl.deliveries.receive() }.id)
        eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }

        delay(500.milliseconds)
        assertNull(impl.deliveries.tryReceive().getOrNull(), "fire must not be executed twice")
    }

    @Test
    fun `failed fire is redelivered and succeeds later`() = withClient { client ->
        RpcDefaults.NAK_BACKOFF = listOf(100.milliseconds)
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val impl = OrderImpl().apply {
            onPlace = { _, context -> check(context!!.deliveryCount > 1) { "first attempt fails" } }
        }
        client.registerService<OrderService>(impl)

        client.service<OrderService>().place(3)

        val first = withTimeout(10.seconds) { impl.deliveries.receive() }
        val second = withTimeout(10.seconds) { impl.deliveries.receive() }
        assertEquals(1, first.context!!.deliveryCount)
        assertEquals(2, second.context!!.deliveryCount)
        assertEquals(first.context.messageId, second.context.messageId, "message id must be stable")

        eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
        assertEquals(0, messageCount(streams.dlqStream))
    }

    @Test
    fun `fire failing on every delivery ends in the dead letter stream`() = withClient { client ->
        RpcDefaults.NAK_BACKOFF = listOf(100.milliseconds)
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val impl = OrderImpl().apply {
            onPlace = { _, _ -> throw IllegalStateException("always") }
        }
        client.registerService<OrderService>(impl)

        client.service<OrderService>().place(9)

        val counts = (1..RpcDefaults.MAX_DELIVER).map {
            withTimeout(10.seconds) { impl.deliveries.receive() }.context!!.deliveryCount
        }
        assertEquals((1L..RpcDefaults.MAX_DELIVER.toLong()).toList(), counts)

        eventually("dead letter must be stored") { messageCount(streams.dlqStream) == 1L }
        eventually("work queue must be empty after term") { messageCount(streams.stream) == 0L }

        val dead = jsm().getLastMessage(streams.dlqStream, streams.dlqSubject)
        val deadHeaders = assertNotNull(dead.headers)
        assertEquals(IllegalStateException::class.java.name, deadHeaders.getFirst(ProtocolHeaders.ERROR))
        assertEquals(
            client.serviceDescriptor(OrderService::class.java).method("place").subject,
            deadHeaders.getFirst(ProtocolHeaders.DEAD_SUBJECT)
        )

        delay(500.milliseconds)
        assertNull(impl.deliveries.tryReceive().getOrNull(), "no delivery after the dead letter")
    }

    @Test
    fun `fire exceeding the max duration ends in the dead letter stream`() = withClient { client ->
        RpcDefaults.FIRE_MAX_DURATION = 200.milliseconds
        RpcDefaults.NAK_BACKOFF = listOf(100.milliseconds)
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val impl = OrderImpl().apply { onPlace = { _, _ -> awaitCancellation() } }
        client.registerService<OrderService>(impl)

        client.service<OrderService>().place(11)

        repeat(RpcDefaults.MAX_DELIVER) { withTimeout(10.seconds) { impl.deliveries.receive() } }
        eventually("dead letter must be stored") { messageCount(streams.dlqStream) == 1L }

        val dead = jsm().getLastMessage(streams.dlqStream, streams.dlqSubject)
        assertEquals(TimeoutException::class.java.name, assertNotNull(dead.headers).getFirst(ProtocolHeaders.ERROR))
    }

    @Test
    fun `timeout inside a fire handler is not reported as exceeding the max duration`() = withClient { client ->
        RpcDefaults.NAK_BACKOFF = listOf(100.milliseconds)
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val impl = OrderImpl().apply {
            onPlace = { _, _ -> withTimeout(10.milliseconds) { delay(1.seconds) } }
        }
        client.registerService<OrderService>(impl)

        client.service<OrderService>().place(12)

        repeat(RpcDefaults.MAX_DELIVER) { withTimeout(10.seconds) { impl.deliveries.receive() } }
        eventually("dead letter must be stored") { messageCount(streams.dlqStream) == 1L }

        val dead = jsm().getLastMessage(streams.dlqStream, streams.dlqSubject)
        assertEquals(
            TimeoutCancellationException::class.java.name,
            assertNotNull(dead.headers).getFirst(ProtocolHeaders.ERROR)
        )
    }

    @Test
    fun `closing the registration cancels its handlers`() = withClient { client ->
        RpcDefaults.ACK_WAIT = 1.seconds
        val started = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val first = client.registerService<OrderService>(OrderImpl().apply {
            onPlace = { _, _ ->
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled.complete(Unit)
                }
            }
        })
        client.service<OrderService>().place(7)
        withTimeout(10.seconds) { started.await() }
        first.close()
        withTimeout(5.seconds) { cancelled.await() }

        val impl = OrderImpl()
        client.registerService<OrderService>(impl).use {
            val delivery = withTimeout(15.seconds) { impl.deliveries.receive() }
            assertEquals(7, delivery.id)
            assertEquals(2, delivery.context!!.deliveryCount)
        }
    }

    @Test
    fun `fire interrupted by a crash is redelivered`() = runTest(timeout = 2.minutes) {
        withContext(Dispatchers.Default) {
            RpcDefaults.ACK_WAIT = 1.seconds
            val name = nextClientName()
            val first = NatsClient.create(name) as NatsClientImpl
            val streams = first.serviceDescriptor(OrderService::class.java).streams!!
            try {
                val started = CompletableDeferred<NatsPersistentContext>()
                first.registerService<OrderService>(OrderImpl().apply {
                    onPlace = { _, context ->
                        started.complete(context!!)
                        awaitCancellation()
                    }
                })
                first.service<OrderService>().place(5)
                val firstContext = withTimeout(10.seconds) { started.await() }
                first.close() // crash: handler cancelled, no ack

                (NatsClient.create(name) as NatsClientImpl).use { second ->
                    val impl = OrderImpl()
                    second.registerService<OrderService>(impl)

                    val delivery = withTimeout(15.seconds) { impl.deliveries.receive() }
                    assertEquals(5, delivery.id)
                    assertEquals(2, delivery.context!!.deliveryCount)
                    assertEquals(firstContext.messageId, delivery.context.messageId)
                    eventually("work queue must be empty after the ack") {
                        messageCount(streams.stream) == 0L
                    }
                }
            } finally {
                first.close()
                deleteStreams(streams)
            }
        }
    }

    @Test
    fun `fire with a missing non-null field is dead-lettered after one delivery`() = withClient { client ->
        RpcDefaults.NAK_BACKOFF = listOf(1.minutes) // a redelivery would not happen within the test
        val descriptor = client.serviceDescriptor(OrderService::class.java)
        val streams = descriptor.streams!!
        val impl = OrderImpl()
        client.registerService<OrderService>(impl)
        val before = delivered(descriptor)

        // A PlaceRequest without its non-null id
        publishExternal(descriptor.method("place").subject, NatsHeaders.EMPTY_BODY)

        eventually("the message must be dead-lettered without waiting for a redelivery") {
            messageCount(streams.dlqStream) == 1L
        }
        eventually("work queue must be empty after term") { messageCount(streams.stream) == 0L }
        assertEquals(1L, delivered(descriptor) - before, "a decode failure must not be redelivered")
        assertNull(impl.deliveries.tryReceive().getOrNull(), "the implementation must not be called")

        val deadHeaders = assertNotNull(jsm().getLastMessage(streams.dlqStream, streams.dlqSubject).headers)
        assertEquals(MissingFieldException::class.java.name, deadHeaders.getFirst(ProtocolHeaders.ERROR))
        assertEquals(descriptor.method("place").subject, deadHeaders.getFirst(ProtocolHeaders.DEAD_SUBJECT))
    }

    @Test
    fun `unknown enum values in a persistent request decode to the zero entry`() = withClient { client ->
        val descriptor = client.serviceDescriptor(OrderService::class.java)
        val streams = descriptor.streams!!
        val impl = OrderImpl()
        client.registerService<OrderService>(impl)

        val sent = WideColorHolder(
            color = WideColor.PURPLE,
            maybeColor = WideColor.PURPLE,
            colors = listOf(WideColor.RED, WideColor.PURPLE),
            colorsByName = mapOf("a" to WideColor.PURPLE, "b" to WideColor.BLUE),
        )
        publishExternal(descriptor.method("paint").subject, NatsCodec.encode(WideColorHolder.serializer(), sent))

        val expected = ColorHolder(
            color = Color.UNSPECIFIED,
            maybeColor = Color.UNSPECIFIED,
            colors = listOf(Color.RED, Color.UNSPECIFIED),
            colorsByName = mapOf("a" to Color.UNSPECIFIED, "b" to Color.BLUE),
        )
        assertEquals(expected, withTimeout(10.seconds) { impl.painted.receive() })
        eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
        assertEquals(0, messageCount(streams.dlqStream))
    }

    // ---- wire contract ----

    @Test
    fun `persistent requests carry the headers of the wire contract`() = withClient { client ->
        val descriptor = client.serviceDescriptor(OrderService::class.java)
        val streams = descriptor.streams!!
        client.registerService<OrderService>(OrderImpl())
        val orders = client.service<OrderService>()

        val connection = NatsConnectionManager.connection()
        val raw = Channel<Message>(Channel.UNLIMITED)
        val dispatcher = connection.createDispatcher()
        dispatcher.subscribe(streams.streamFilter) { raw.trySend(it) }
        connection.flush(java.time.Duration.ofSeconds(5))

        try {
            orders.place(1)
            val fire = withTimeout(10.seconds) { raw.receive() }
            assertEquals(descriptor.method("place").subject, fire.subject)
            assertCommonHeaders(client, streams.stream, fire)
            assertNull(fire.headers.getFirst(ProtocolHeaders.DEADLINE), "persistent fire has no deadline")
            assertNull(fire.headers.getFirst(ProtocolHeaders.REPLY_TO))
            assertNull(fire.headers.getFirst(ProtocolHeaders.NATS_TTL), "persistent fire has no TTL")

            val sentAt = System.currentTimeMillis()
            assertEquals(Number(20), withNatsCallTimeout(10.seconds) { orders.total(2) })
            val rpc = withTimeout(10.seconds) { raw.receive() }
            assertEquals(descriptor.method("total").subject, rpc.subject)
            assertCommonHeaders(client, streams.stream, rpc)
            val deadline = assertNotNull(rpc.headers.getFirst(ProtocolHeaders.DEADLINE)).toLong()
            assertTrue(deadline in sentAt + 9_000..System.currentTimeMillis() + 10_000, "deadline $deadline")
            assertTrue(assertNotNull(rpc.headers.getFirst(ProtocolHeaders.REPLY_TO)).startsWith("_INBOX."))
            assertEquals("10s", rpc.headers.getFirst(ProtocolHeaders.NATS_TTL))

            assertEquals(Number(3), orders.audit(3))
            val audit = withTimeout(10.seconds) { raw.receive() }
            assertEquals("180s", audit.headers.getFirst(ProtocolHeaders.NATS_TTL), "Nats-TTL follows the call timeout")
        } finally {
            connection.closeDispatcher(dispatcher)
        }
    }

    private fun assertCommonHeaders(client: NatsClientImpl, stream: String, message: Message) {
        val headers = assertNotNull(message.headers)
        assertEquals(ProtocolHeaders.PROTOBUF_CONTENT_TYPE, headers.getFirst(ProtocolHeaders.CONTENT_TYPE))
        assertEquals(client.origin, headers.getFirst(ProtocolHeaders.ORIGIN))
        assertEquals(NatsConstants.VERSION, headers.getFirst(ProtocolHeaders.SENDER_VERSION))
        UUID.fromString(assertNotNull(headers.getFirst(ProtocolHeaders.NATS_MSG_ID), "Nats-Msg-Id missing"))
        assertEquals(stream, headers.getFirst(NatsJetStreamConstants.EXPECTED_STREAM_HDR))
    }

    // ---- persistent RPC ----

    @Test
    fun `persistent rpc roundtrips with message context`() = withClient { client ->
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val impl = OrderImpl()
        client.registerService<OrderService>(impl)

        assertEquals(Number(30), withNatsCallTimeout(10.seconds) { client.service<OrderService>().total(3) })
        val context = assertNotNull(impl.totalContexts.tryReceive().getOrNull())
        assertEquals(1, context.deliveryCount)
        eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
    }

    @Test
    fun `persistent rpc with an all-absent response returns it instead of timing out`() =
        withClient { client ->
            client.registerService<OrderService>(OrderImpl())
            val orders = client.service<OrderService>()

            // LookupReply(null) is a 0-byte body
            assertEquals(LookupReply(null), withNatsCallTimeout(10.seconds) { orders.lookup(0) })
            assertEquals(LookupReply("found-5"), withNatsCallTimeout(10.seconds) { orders.lookup(5) })
        }

    @Test
    fun `persistent rpc waits for a responder that starts within the timeout`() =
        withClient { client ->
            val orders = client.service<OrderService>()
            val start = TimeSource.Monotonic.markNow()
            val result = async { withNatsCallTimeout(15.seconds) { orders.total(4) } }

            delay(1500.milliseconds)
            assertFalse(result.isCompleted)
            client.registerService<OrderService>(OrderImpl())

            assertEquals(Number(40), result.await())
            assertTrue(start.elapsedNow() >= 1500.milliseconds)
        }

    @Test
    fun `expired persistent rpc is never executed`() = withClient { client ->
        val orders = client.service<OrderService>()
        assertFailsWith<NatsCallTimeoutException> {
            withNatsCallTimeout(1.seconds) { orders.total(6) }
        }

        delay(1500.milliseconds)
        val impl = OrderImpl()
        client.registerService<OrderService>(impl)
        delay(1500.milliseconds)

        assertEquals(0, impl.totalCalls.get(), "expired request must not run")
    }

    @Test
    fun `persistent rpc past its deadline is terminated without running`() = withClient { client ->
        val descriptor = client.serviceDescriptor(OrderService::class.java)
        val streams = descriptor.streams!!
        val impl = OrderImpl()
        client.registerService<OrderService>(impl)

        // Published like an external caller without Nats-TTL, so only the responder's deadline check stops it
        val method = descriptor.method("total")
        val headers = NatsHeaders.base("external")
            .add(ProtocolHeaders.DEADLINE, (System.currentTimeMillis() - 1_000).toString())
        val body = NatsCodec.encode(TotalRequest.serializer(), TotalRequest(6))
        NatsConnectionManager.connection().jetStream().publish(method.subject, headers, body)

        eventually("the expired request must be removed from the work queue") { messageCount(streams.stream) == 0L }
        assertEquals(0, impl.totalCalls.get(), "expired request must not run")
    }

    @Test
    fun `persistent rpc slower than its timeout is cancelled and terminated`() = withClient { client ->
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val cancelled = CompletableDeferred<Unit>()
        val impl = OrderImpl().apply {
            onTotal = { id ->
                try {
                    delay(10.seconds)
                    id
                } catch (e: CancellationException) {
                    cancelled.complete(Unit)
                    throw e
                }
            }
        }
        client.registerService<OrderService>(impl)

        assertFailsWith<NatsCallTimeoutException> {
            withNatsCallTimeout(1.seconds) { client.service<OrderService>().total(7) }
        }
        withTimeout(5.seconds) { cancelled.await() }
        assertEquals(1, impl.totalCalls.get())
        eventually("the timed out request must be removed from the work queue") { messageCount(streams.stream) == 0L }
    }

    @Test
    fun `rejected persistent publish is a NatsRpcException`() = withClient { client ->
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val orders = client.service<OrderService>()
        withNatsCallTimeout(10.seconds) { orders.place(1) } // provisions the stream
        jsm().deleteStream(streams.stream) // the provisioning cache does not notice this

        val error = assertFailsWith<NatsRpcException> {
            withNatsCallTimeout(10.seconds) { orders.place(2) }
        }
        // Coroutine stack trace recovery (tests run with assertions) may put a copy in front of the original
        assertTrue(generateSequence(error.cause) { it.cause }.any { it is IOException }, error.stackTraceToString())
    }

    @Test
    fun `persistent rpc failure is answered and not redelivered`() = withClient { client ->
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val impl = OrderImpl().apply { onTotal = { throw IllegalStateException("no total") } }
        client.registerService<OrderService>(impl)

        val exception = assertFailsWith<NatsRemoteException> {
            withNatsCallTimeout(10.seconds) { client.service<OrderService>().total(1) }
        }
        assertEquals(IllegalStateException::class.java.name, exception.remoteType)
        assertEquals("no total", exception.remoteMessage)

        eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
        delay(500.milliseconds)
        assertEquals(1, impl.totalCalls.get())
    }

    @Test
    fun `persistent rpc with a missing non-null field is answered with an error and acked`() = withClient { client ->
        val descriptor = client.serviceDescriptor(OrderService::class.java)
        val streams = descriptor.streams!!
        val impl = OrderImpl()
        client.registerService<OrderService>(impl)
        val before = delivered(descriptor)

        val connection = NatsConnectionManager.connection()
        val inbox = connection.createInbox()
        val replies = connection.subscribe(inbox)
        connection.flush(java.time.Duration.ofSeconds(5))
        try {
            // A TotalRequest without its non-null id
            publishExternal(
                descriptor.method("total").subject,
                NatsHeaders.EMPTY_BODY,
                extraHeaders = mapOf(
                    ProtocolHeaders.REPLY_TO to inbox,
                    ProtocolHeaders.DEADLINE to (System.currentTimeMillis() + 10_000).toString(),
                ),
            )

            val reply = assertNotNull(replies.nextMessage(java.time.Duration.ofSeconds(10)), "no error reply")
            assertEquals(MissingFieldException::class.java.name, reply.headers.getFirst(ProtocolHeaders.ERROR))
        } finally {
            replies.unsubscribe()
        }

        eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
        delay(500.milliseconds)
        assertEquals(1L, delivered(descriptor) - before, "the request must not be redelivered")
        assertEquals(0, messageCount(streams.dlqStream), "a persistent rpc is never dead-lettered")
        assertEquals(0, impl.totalCalls.get(), "the implementation must not be called")
    }

    @Test
    fun `persistent fire timeout above the duplicate window is rejected before publishing`() =
        withClient { client ->
            val streams = client.serviceDescriptor(OrderService::class.java).streams!!
            JetStreamProvisioner.ensureStreams(streams)
            val orders = client.service<OrderService>()

            assertFailsWith<IllegalArgumentException> { withNatsCallTimeout(3.minutes) { orders.place(1) } }

            assertEquals(0L, messageCount(streams.stream), "nothing must be published")
        }

    @Test
    fun `persistent call with a result waits for a reply beyond the duplicate window`() = withClient { client ->
        val impl = OrderImpl().apply {
            onTotal = { id ->
                delay(2.seconds)
                id * 10
            }
        }
        client.registerService<OrderService>(impl)
        val orders = client.service<OrderService>()
        RuntimeDefaults.dedupeWindow = 1.seconds // only bounds the publish phase

        assertEquals(Number(40), withNatsCallTimeout(5.seconds) { orders.total(4) })
    }

    @Test
    fun `publish phase of a persistent call with a result ends at the duplicate window`() = withClient { client ->
        val descriptor = client.serviceDescriptor(OrderService::class.java)
        val streams = descriptor.streams!!
        JetStreamProvisioner.ensureStreams(streams)
        val config = jsm().getStreamInfo(streams.stream).configuration
        jsm().updateStream(StreamConfiguration.builder(config).noAck(true).build())
        RuntimeDefaults.dedupeWindow = 1.seconds

        val start = TimeSource.Monotonic.markNow()
        val exception = assertFailsWith<NatsCallTimeoutException> {
            withNatsCallTimeout(10.seconds) { client.service<OrderService>().total(1) }
        }
        val took = start.elapsedNow()

        assertEquals(10.seconds, exception.timeout)
        assertTrue(took < 5.seconds, "the publish phase must end at the duplicate window, took $took")
    }

    @Test
    fun `call timeout above the duplicate window is allowed for core methods`() = withClient { client ->
        client.registerService<OrderService>(OrderImpl())

        assertEquals(Text("pong"), withNatsCallTimeout(3.minutes) { client.service<OrderService>().ping() })
    }

    @Test
    fun `missing PubAck is retried with the same message id and stores one message`() = withClient { client ->
        val descriptor = client.serviceDescriptor(OrderService::class.java)
        val streams = descriptor.streams!!
        JetStreamProvisioner.ensureStreams(streams)
        // The stream still stores and dedupes by Nats-Msg-Id, but never acknowledges a publish
        val config = jsm().getStreamInfo(streams.stream).configuration
        jsm().updateStream(StreamConfiguration.builder(config).noAck(true).build())

        val connection = NatsConnectionManager.connection()
        val raw = ConcurrentLinkedQueue<Message>()
        val dispatcher = connection.createDispatcher()
        dispatcher.subscribe(descriptor.method("place").subject) { raw += it }
        connection.flush(java.time.Duration.ofSeconds(5))

        try {
            val start = TimeSource.Monotonic.markNow()
            assertFailsWith<NatsCallTimeoutException> {
                withNatsCallTimeout(12.seconds) { client.service<OrderService>().place(1) }
            }
            assertTrue(start.elapsedNow() >= 11500.milliseconds, "took ${start.elapsedNow()}")

            eventually("the publish must have been repeated") { raw.size >= 2 }
            val ids = raw.map { it.headers.getFirst(NatsJetStreamConstants.MSG_ID_HDR) }
            assertNotNull(ids.first())
            assertEquals(1, ids.toSet().size, "every attempt must carry the same Nats-Msg-Id: $ids")
            assertEquals(1L, messageCount(streams.stream), "the stream must dedupe the repeated publishes")
        } finally {
            connection.closeDispatcher(dispatcher)
        }
    }

    // ---- versions ----

    @Test
    fun `major versions of a persistent service use separate streams`() = withClient { client ->
        val v1 = client.serviceDescriptor(VersionedJournalServiceV1::class.java)
        val v2 = client.serviceDescriptor(VersionedJournalServiceV2::class.java)
        val streams1 = v1.streams!!
        val streams2 = v2.streams!!
        try {
            assertEquals("SURF_PERSIST_surf_nats_test_v1_VersionedJournalService", streams1.stream)
            assertEquals("SURF_PERSIST_surf_nats_test_v2_VersionedJournalService", streams2.stream)
            assertEquals("surf.persist.surf.nats.test.v1.VersionedJournalService.*", streams1.streamFilter)
            assertEquals("surf.persist.surf.nats.test.v2.VersionedJournalService.*", streams2.streamFilter)
            assertEquals(streams1.durable, streams2.durable, "durables stay unversioned, one per stream")

            val journal1 = JournalV1()
            val journal2 = JournalV2()
            client.registerService<VersionedJournalServiceV1>(journal1)
            client.registerService<VersionedJournalServiceV2>(journal2)
            assertEquals(listOf(streams1.streamFilter), jsm().getStreamInfo(streams1.stream).configuration.subjects)
            assertEquals(listOf(streams2.streamFilter), jsm().getStreamInfo(streams2.stream).configuration.subjects)

            val service1 = client.service<VersionedJournalServiceV1>()
            val service2 = client.service<VersionedJournalServiceV2>()
            assertEquals(VersionedPingReplyV1("a", 1), withNatsCallTimeout(10.seconds) { service1.append("a") })
            assertEquals(
                VersionedPingReplyV2("b", 2, "attempt-1"),
                withNatsCallTimeout(10.seconds) { service2.append("b") },
            )

            service1.record("one")
            service2.record("two", attempt = 3)
            assertEquals("one", withTimeout(10.seconds) { journal1.recorded.receive() })
            assertEquals("two" to 3, withTimeout(10.seconds) { journal2.recorded.receive() })
            eventually("both work queues must be drained") {
                messageCount(streams1.stream) == 0L && messageCount(streams2.stream) == 0L
            }

            delay(300.milliseconds)
            assertNull(journal1.recorded.tryReceive().getOrNull(), "v1 must not receive v2 calls")
            assertNull(journal2.recorded.tryReceive().getOrNull(), "v2 must not receive v1 calls")
        } finally {
            client.close()
            deleteStreams(streams1)
            deleteStreams(streams2)
        }
    }

    // ---- large payloads ----

    @Test
    fun `large fire payload goes through the persistent bucket and is deleted`() =
        withClient { client ->
            val impl = OrderImpl()
            client.registerService<OrderService>(impl)
            val before = liveObjects(PayloadBucket.PERSISTENT)
            val data = blob(LARGE)

            client.service<OrderService>().big(data)

            assertContentEquals(data, withTimeout(30.seconds) { impl.bigReceived.receive() })
            eventually("persistent payload must be deleted after the ack") {
                liveObjects(PayloadBucket.PERSISTENT) == before
            }
        }

    @Test
    fun `large persistent rpc payload roundtrips and is deleted`() = withClient { client ->
        client.registerService<OrderService>(OrderImpl())
        val before = liveObjects(PayloadBucket.TRANSIENT)
        val data = blob(LARGE)

        val echoed = withNatsCallTimeout(30.seconds) { client.service<OrderService>().echo(data) }
        assertContentEquals(data, echoed.data)
        eventually("transient payloads must be deleted") { liveObjects(PayloadBucket.TRANSIENT) == before }
    }

    // ---- helpers ----

    private fun nextClientName() = "persist-${clientCounter++}-${UUID.randomUUID().toString().take(6)}"

    private fun withClient(block: suspend CoroutineScope.(NatsClientImpl) -> Unit) =
        runTest(timeout = 2.minutes) {
            (NatsClient.create(nextClientName()) as NatsClientImpl).use { client ->
                val streams = client.serviceDescriptor(OrderService::class.java).streams!!
                try {
                    withContext(Dispatchers.Default) { block(client) }
                } finally {
                    client.close()
                    deleteStreams(streams)
                }
            }
        }

    private fun ServiceDescriptor.method(name: String) = methods.single { it.kotlinName == name }

    private fun delivered(descriptor: ServiceDescriptor): Long {
        val streams = descriptor.streams!!
        return jsm().getConsumerInfo(streams.stream, streams.durable).delivered.consumerSequence
    }

    // Publishes like an external producer: protobuf Content-Type and a Nats-Msg-Id, no Surf headers
    private fun publishExternal(subject: String, body: ByteArray, extraHeaders: Map<String, String> = emptyMap()) {
        val headers = Headers()
            .add(NatsJetStreamConstants.MSG_ID_HDR, UUID.randomUUID().toString())
            .add(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
        extraHeaders.forEach { (name, value) -> headers.add(name, value) }
        NatsConnectionManager.connection().jetStream().publish(subject, headers, body)
    }

    // All tests share the streams of a service; the provisioning cache must forget them to create them again
    private fun deleteStreams(streams: ServiceNames) {
        for (stream in listOf(streams.stream, streams.dlqStream)) runCatching { jsm().deleteStream(stream) }
        JetStreamProvisioner.invalidate(streams)
    }

    private fun liveObjects(bucket: PayloadBucket): Set<String> {
        val connection = NatsConnectionManager.connection()
        if (bucket.bucketName !in connection.objectStoreManagement().bucketNames) return emptySet()
        return connection.objectStore(bucket.bucketName).list
            .filterNot { it.isDeleted }
            .mapTo(HashSet()) { it.objectName }
    }

    companion object {
        private const val LARGE = 3 * 1024 * 1024

        fun blob(size: Int) = ByteArray(size) { (it * 31 + it / 7).toByte() }
    }
}
