package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.eventually
import dev.slne.surf.nats.jetstream.JetStreamProvisioner
import dev.slne.surf.nats.jsm
import dev.slne.surf.nats.messageCount
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.registerService
import dev.slne.surf.nats.rpc.NatsPersistentServiceTest.OrderImpl
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.proto.v1.Number
import dev.slne.surf.nats.test.proto.v1.OrderService
import dev.slne.surf.nats.test.proto.v1.PlaceRequest
import dev.slne.surf.nats.test.proto.v1.Text
import dev.slne.surf.nats.test.proto.v1.TotalRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.security.MessageDigest
import java.util.HexFormat
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersistentInterceptorTest {

    private class Transaction(val name: String) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<Transaction>
    }

    private class Seen(
        val serviceName: String,
        val methodName: String,
        val messageId: String,
        val deliveryCount: Long,
        val requestFingerprint: String,
    )

    private val defaultBackoff = RpcDefaults.NAK_BACKOFF

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @AfterEach
    fun resetDefaults() {
        RpcDefaults.NAK_BACKOFF = defaultBackoff
    }

    @Test
    fun `interceptor sees the method identity, message id and request fingerprint`() = withClient { client ->
        val seen = Channel<Seen>(Channel.UNLIMITED)
        val impl = OrderImpl()
        client.registerService<OrderService>(impl) { call ->
            seen.send(Seen(call.serviceName, call.methodName, call.messageId, call.deliveryCount, call.requestFingerprint))
            call.proceed()
        }

        assertEquals(Number(30), total(client, 3))

        val call = seen.receive()
        assertEquals("surf.nats.test.v1.OrderService", call.serviceName)
        assertEquals("Total", call.methodName)
        assertEquals(impl.totalContexts.receive()!!.messageId, call.messageId)
        assertEquals(1, call.deliveryCount)
        assertEquals(sha256(NatsCodec.encode(TotalRequest.serializer(), TotalRequest(3))), call.requestFingerprint)
    }

    @Test
    fun `core methods do not run through the interceptor`() = withClient { client ->
        val intercepted = Channel<String>(Channel.UNLIMITED)
        client.registerService<OrderService>(OrderImpl()) { call ->
            intercepted.send(call.methodName)
            call.proceed()
        }

        assertEquals(Text("pong"), client.service<OrderService>().ping())
        delay(200.milliseconds)
        assertNull(intercepted.tryReceive().getOrNull())
    }

    @Test
    fun `stored reply answers a retry without running the handler`() = withClient { client ->
        val impl = OrderImpl()
        val stored = AtomicReference<Pair<String, ByteArray>>()
        client.registerService<OrderService>(impl) { call ->
            stored.get()?.let { (format, bytes) -> return@registerService NatsPersistentReply.restore(format, bytes) }
            call.proceed().also { stored.set(it.format to it.bytes) }
        }

        assertEquals(Number(30), total(client, 3))
        impl.onTotal = { it * 100 }
        assertEquals(Number(30), total(client, 3), "the retry must get the stored reply")
        assertEquals(1, impl.totalCalls.get())
    }

    @Test
    fun `handler runs in the coroutine context of the interceptor`() = withClient { client ->
        val transaction = CompletableDeferred<Transaction?>()
        val impl = OrderImpl().apply {
            onTotal = { id ->
                transaction.complete(currentCoroutineContext()[Transaction])
                id
            }
        }
        client.registerService<OrderService>(impl) { call ->
            withContext(Transaction("tx-1")) { call.proceed() }
        }

        assertEquals(Number(4), total(client, 4))
        assertEquals("tx-1", transaction.await()?.name)
    }

    @Test
    fun `proceeding twice is rejected`() = withClient { client ->
        client.registerService<OrderService>(OrderImpl()) { call ->
            call.proceed()
            call.proceed()
        }

        val exception = assertFailsWith<NatsRemoteException> { total(client, 1) }
        assertEquals(IllegalStateException::class.java.name, exception.remoteType)
    }

    @Test
    fun `persistent rpc requesting a redelivery runs again and answers once`() = withClient { client ->
        RpcDefaults.NAK_BACKOFF = listOf(100.milliseconds)
        val deliveries = ConcurrentLinkedQueue<Long>()
        val impl = OrderImpl()
        client.registerService<OrderService>(impl) { call ->
            deliveries += call.deliveryCount
            if (call.deliveryCount == 1L) throw NatsRedeliveryException("database unavailable")
            call.proceed()
        }

        assertEquals(Number(50), total(client, 5))
        assertEquals(listOf(1L, 2L), deliveries.toList())
        assertEquals(1, impl.totalCalls.get())
    }

    @Test
    fun `persistent rpc requesting a redelivery on its last delivery is answered with the error`() =
        withClient { client ->
            RpcDefaults.NAK_BACKOFF = listOf(100.milliseconds)
            val streams = client.serviceDescriptor(OrderService::class.java).streams!!
            val deliveries = ConcurrentLinkedQueue<Long>()
            client.registerService<OrderService>(OrderImpl()) { call ->
                deliveries += call.deliveryCount
                throw NatsRedeliveryException("database unavailable")
            }

            val exception = assertFailsWith<NatsRemoteException> { total(client, 1) }
            assertEquals(NatsRedeliveryException::class.java.name, exception.remoteType)
            assertEquals("database unavailable", exception.remoteMessage)
            assertEquals((1L..RpcDefaults.MAX_DELIVER.toLong()).toList(), deliveries.toList())
            eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
        }

    @Test
    fun `persistent rpc requesting a redelivery after its deadline is answered with the error`() =
        withClient { client ->
            RpcDefaults.NAK_BACKOFF = listOf(1.minutes)
            val deliveries = ConcurrentLinkedQueue<Long>()
            client.registerService<OrderService>(OrderImpl()) { call ->
                deliveries += call.deliveryCount
                throw NatsRedeliveryException("database unavailable")
            }

            val exception = assertFailsWith<NatsRemoteException> { total(client, 1) }
            assertEquals(NatsRedeliveryException::class.java.name, exception.remoteType)
            assertEquals(listOf(1L), deliveries.toList())
        }

    @Test
    fun `stored error reply is answered as a remote exception`() = withClient { client ->
        val impl = OrderImpl()
        client.registerService<OrderService>(impl) { _ ->
            NatsPersistentReply.error(IllegalArgumentException("duplicate request"))
        }

        val exception = assertFailsWith<NatsRemoteException> { total(client, 1) }
        assertEquals(IllegalArgumentException::class.java.name, exception.remoteType)
        assertEquals("duplicate request", exception.remoteMessage)
        assertEquals(0, impl.totalCalls.get())
    }

    @Test
    fun `reply that does not fit the method is answered with an error`() = withClient { client ->
        client.registerService<OrderService>(OrderImpl()) { _ -> NatsPersistentReply.empty() }

        val exception = assertFailsWith<NatsRemoteException> { total(client, 1) }
        assertEquals(IllegalStateException::class.java.name, exception.remoteType)
    }

    @Test
    fun `fire with a stored reply is acknowledged without running the handler`() = withClient { client ->
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val impl = OrderImpl()
        val stored = AtomicReference<NatsPersistentReply>()
        client.registerService<OrderService>(impl) { call ->
            stored.get() ?: call.proceed().also { stored.set(it) }
        }
        val orders = client.service<OrderService>()

        orders.place(PlaceRequest(1))
        assertEquals(1, withTimeout(10.seconds) { impl.deliveries.receive() }.id)
        eventually("the first fire must be stored") { stored.get() != null }

        orders.place(PlaceRequest(2))
        eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
        delay(300.milliseconds)
        assertNull(impl.deliveries.tryReceive().getOrNull(), "the handler must not run for the stored fire")
        assertEquals(0, messageCount(streams.dlqStream))
    }

    @Test
    fun `fire requesting a redelivery is delivered again`() = withClient { client ->
        RpcDefaults.NAK_BACKOFF = listOf(100.milliseconds)
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val impl = OrderImpl()
        client.registerService<OrderService>(impl) { call ->
            if (call.deliveryCount == 1L) throw NatsRedeliveryException("database unavailable")
            call.proceed()
        }

        client.service<OrderService>().place(PlaceRequest(3))

        val delivery = withTimeout(10.seconds) { impl.deliveries.receive() }
        assertEquals(2, delivery.context!!.deliveryCount)
        eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
        assertEquals(0, messageCount(streams.dlqStream))
    }

    @Test
    fun `fire with a stored error reply is dead-lettered without redelivery`() = withClient { client ->
        RpcDefaults.NAK_BACKOFF = listOf(1.minutes) // a redelivery would not happen within the test
        val streams = client.serviceDescriptor(OrderService::class.java).streams!!
        val impl = OrderImpl()
        client.registerService<OrderService>(impl) { _ ->
            NatsPersistentReply.error(IllegalArgumentException("rejected before"))
        }

        client.service<OrderService>().place(PlaceRequest(4))

        eventually("dead letter must be stored") { messageCount(streams.dlqStream) == 1L }
        eventually("work queue must be empty after term") { messageCount(streams.stream) == 0L }
        val deadHeaders = assertNotNull(jsm().getLastMessage(streams.dlqStream, streams.dlqSubject).headers)
        assertEquals(NatsRemoteException::class.java.name, deadHeaders.getFirst(ProtocolHeaders.ERROR))
        assertNull(impl.deliveries.tryReceive().getOrNull())
    }

    private suspend fun total(client: NatsClient, id: Long): Number {
        return withNatsCallTimeout(10.seconds) { client.service<OrderService>().total(TotalRequest(id)) }
    }

    private fun sha256(bytes: ByteArray): String =
        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes))

    private fun withClient(block: suspend CoroutineScope.(NatsClientImpl) -> Unit) =
        runTest(timeout = 2.minutes) {
            val name = "intercept-${UUID.randomUUID().toString().take(8)}"
            (NatsClient.create(name) as NatsClientImpl).use { client ->
                val streams = client.serviceDescriptor(OrderService::class.java).streams!!
                try {
                    withContext(Dispatchers.Default) { block(client) }
                } finally {
                    client.close()
                    for (stream in listOf(streams.stream, streams.dlqStream)) runCatching { jsm().deleteStream(stream) }
                    JetStreamProvisioner.invalidate(streams)
                }
            }
        }
}
