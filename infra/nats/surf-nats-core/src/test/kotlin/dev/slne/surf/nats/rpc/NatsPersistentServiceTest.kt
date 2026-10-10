package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.rpc.payload.PayloadBucket
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NatsPersistentServiceTest {

    @NatsService("orders")
    @NatsPersistent
    interface OrderService {
        suspend fun place(id: Long)
        suspend fun total(id: Long): Long
        suspend fun big(data: ByteArray)
        suspend fun echo(data: ByteArray): ByteArray

        @NatsPersistent(false)
        suspend fun ping(): String
    }

    class Delivery(val id: Long, val context: NatsPersistentMessageContext?)

    class OrderImpl : OrderService {
        val deliveries = Channel<Delivery>(Channel.UNLIMITED)
        val bigReceived = Channel<ByteArray>(Channel.UNLIMITED)
        val totalCalls = AtomicInteger()
        val totalContexts = Channel<NatsPersistentMessageContext?>(Channel.UNLIMITED)

        @Volatile
        var onPlace: suspend (Long, NatsPersistentMessageContext?) -> Unit = { _, _ -> }

        @Volatile
        var onTotal: suspend (Long) -> Long = { it * 10 }

        override suspend fun place(id: Long) {
            val context = natsPersistentMessageContextOrNull()
            deliveries.send(Delivery(id, context))
            onPlace(id, context)
        }

        override suspend fun total(id: Long): Long {
            totalCalls.incrementAndGet()
            totalContexts.send(natsPersistentMessageContextOrNull())
            return onTotal(id)
        }

        override suspend fun big(data: ByteArray) {
            bigReceived.send(data)
        }

        override suspend fun echo(data: ByteArray): ByteArray = data

        override suspend fun ping(): String = "pong"
    }

    @NatsService("timed")
    @NatsPersistent
    @NatsTimeout(42)
    interface TimedService {
        suspend fun inherited(): Long

        @NatsTimeout(3)
        suspend fun own(): Long

        @NatsPersistent(false)
        suspend fun core(): Long
    }

    @NatsService("plain")
    interface PlainService {
        suspend fun core(): Long

        @NatsPersistent
        suspend fun stored(id: Long)
    }

    @NatsService("bad-all")
    @NatsPersistent
    interface PersistentAllService {
        @NatsResponders(ResponderMode.ALL)
        suspend fun find(id: Long): Answer<String>
    }

    @NatsService("mixed")
    @NatsPersistent
    interface MixedService {
        suspend fun store(id: Long)

        @NatsPersistent(false)
        @NatsResponders(ResponderMode.ALL)
        suspend fun find(id: Long): Answer<String>
    }

    private var clientCounter = 0
    private val defaultAckWait = RpcDefaults.ACK_WAIT
    private val defaultBackoff = RpcDefaults.NAK_BACKOFF

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @AfterEach
    fun resetDefaults() {
        RpcDefaults.ACK_WAIT = defaultAckWait
        RpcDefaults.NAK_BACKOFF = defaultBackoff
    }

    // ---- descriptor ----

    @Test
    fun `persistent methods use the persist namespace and core methods stay unchanged`() =
        withClient { client ->
            val descriptor = client.serviceDescriptor(OrderService::class.java)
            val place = descriptor.method("place")
            val ping = descriptor.method("ping")

            assertTrue(place.persistent)
            assertEquals("${client.name}.\$persist.orders.place", place.subject)
            assertFalse(ping.persistent)
            assertEquals("${client.name}.orders.ping", ping.subject)

            val streams = assertNotNull(descriptor.streams)
            val prefix = "SURF_${client.name}_ORDERS".uppercase().replace('-', '_')
            assertEquals(prefix, streams.stream)
            assertEquals("${client.name}.\$persist.orders.>", streams.filter)
            assertEquals("${prefix}_DLQ", streams.dlqStream)
            assertEquals("${client.name}.\$dlq.orders", streams.dlqSubject)
        }

    @Test
    fun `timeouts resolve from method, interface and default`() = withClient { client ->
        val timed = client.serviceDescriptor(TimedService::class.java)
        assertEquals(42.seconds, timed.method("inherited").timeout)
        assertEquals(3.seconds, timed.method("own").timeout)
        assertEquals(42.seconds, timed.method("core").timeout)

        val orders = client.serviceDescriptor(OrderService::class.java)
        assertEquals(RpcDefaults.JETSTREAM_TIMEOUT, orders.method("total").timeout)
        assertEquals(RpcDefaults.CORE_TIMEOUT, orders.method("ping").timeout)
    }

    @Test
    fun `method level Persistent works without the interface annotation`() = withClient { client ->
        val plain = client.serviceDescriptor(PlainService::class.java)
        assertFalse(plain.method("core").persistent)
        assertTrue(plain.method("stored").persistent)
        assertNotNull(plain.streams)
    }

    @Test
    fun `persistent ALL method is rejected`() = withClient { client ->
        val message = assertFailsWith<IllegalArgumentException> {
            client.serviceDescriptor(PersistentAllService::class.java)
        }.message.orEmpty()
        assertContains(message, "@NatsPersistent")
    }

    @Test
    fun `ALL method opted out of Persistent is accepted`() = withClient { client ->
        val mixed = client.serviceDescriptor(MixedService::class.java)
        assertTrue(mixed.method("store").persistent)
        assertFalse(mixed.method("find").persistent)
        assertNotNull(mixed.presenceSubject)
    }

    @Test
    fun `service without persistent methods has no streams`() = withClient { client ->
        assertNull(client.serviceDescriptor(ServiceResponderTest.CalcService::class.java).streams)
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
            val context = assertNotNull(delivery.context, "NatsPersistentMessageContext missing")
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

        assertEquals("pong", orders.ping())
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
        assertEquals(IllegalStateException::class.java.name, deadHeaders.getFirst(NatsHeaders.ERROR))
        assertEquals(
            client.serviceDescriptor(OrderService::class.java).method("place").subject,
            deadHeaders.getFirst(NatsHeaders.DEAD_SUBJECT)
        )

        delay(500.milliseconds)
        assertNull(impl.deliveries.tryReceive().getOrNull(), "no delivery after the dead letter")
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
                val started = CompletableDeferred<NatsPersistentMessageContext>()
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
                deleteStreams(streams.stream, streams.dlqStream)
            }
        }
    }

    // ---- persistent RPC ----

    @Test
    fun `persistent rpc roundtrips with message context`() = withClient { client ->
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val impl = OrderImpl()
        client.registerService<OrderService>(impl)

        assertEquals(30, withNatsCallTimeout(10.seconds) { client.service<OrderService>().total(3) })
        val context = assertNotNull(impl.totalContexts.tryReceive().getOrNull())
        assertEquals(1, context.deliveryCount)
        eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
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

            assertEquals(40, result.await())
            assertTrue(start.elapsedNow() >= 1500.milliseconds)
        }

    @Test
    fun `expired persistent rpc is never executed`() = withClient { client ->
        val orders = client.service<OrderService>()
        assertFailsWith<NatsRequestTimeoutException> {
            withNatsCallTimeout(1.seconds) { orders.total(6) }
        }

        delay(1500.milliseconds)
        val impl = OrderImpl()
        client.registerService<OrderService>(impl)
        delay(1500.milliseconds)

        assertEquals(0, impl.totalCalls.get(), "expired request must not run")
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

        assertContentEquals(data, withNatsCallTimeout(30.seconds) { client.service<OrderService>().echo(data) })
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
                    deleteStreams(streams.stream, streams.dlqStream)
                }
            }
        }

    private fun ServiceDescriptor.method(name: String) = methods.single { it.method.name == name }

    private fun jsm() = NatsConnectionManager.connection().jetStreamManagement()

    private fun messageCount(stream: String): Long =
        jsm().getStreamInfo(stream).streamState.msgCount

    private fun deleteStreams(vararg streams: String) {
        for (stream in streams) runCatching { jsm().deleteStream(stream) }
    }

    private fun liveObjects(bucket: PayloadBucket): Set<String> {
        val connection = NatsConnectionManager.connection()
        if (bucket.bucketName !in connection.objectStoreManagement().bucketNames) return emptySet()
        return connection.objectStore(bucket.bucketName).list
            .filterNot { it.isDeleted }
            .mapTo(HashSet()) { it.objectName }
    }

    private suspend fun eventually(
        message: String,
        timeout: Duration = 10.seconds,
        condition: suspend () -> Boolean,
    ) {
        val start = TimeSource.Monotonic.markNow()
        while (!condition()) {
            if (start.elapsedNow() > timeout) fail("$message (waited $timeout)")
            delay(50.milliseconds)
        }
    }

    companion object {
        private const val LARGE = 3 * 1024 * 1024

        fun blob(size: Int) = ByteArray(size) { (it * 31 + it / 7).toByte() }
    }
}
