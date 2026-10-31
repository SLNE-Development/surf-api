package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.jetstream.JetStreamProvisioner
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.test.proto.v1.EchoRequest
import dev.slne.surf.nats.test.proto.v1.LateService
import dev.slne.surf.nats.test.proto.v1.Number
import dev.slne.surf.nats.test.proto.v1.OwnerReply
import dev.slne.surf.nats.test.proto.v1.PutRequest
import dev.slne.surf.nats.test.proto.v1.RolesService
import dev.slne.surf.nats.test.proto.v1.WhitelistRequest
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.impl.Headers
import io.nats.client.support.NatsJetStreamConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CyclicBarrier
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExternalCallerTest {

    class RolesImpl : RolesService {
        val whitelisted = Channel<Pair<String, Long>>(Channel.UNLIMITED)
        val stored = ConcurrentLinkedQueue<Long>()
        val handled = ConcurrentLinkedQueue<Long>()

        override suspend fun whitelist(action: String, userId: Long) {
            whitelisted.send(action to userId)
        }

        override suspend fun echo(id: Long): Number = Number(id)

        override suspend fun owner(player: UUID): OwnerReply = OwnerReply(player)

        override suspend fun store(id: Long, payload: String?) {
            stored += id
        }

        override suspend fun handle(id: Long, payload: String?): Number {
            handled += id
            return Number(id)
        }
    }

    class LateImpl : LateService {
        val stored = Channel<Long>(Channel.UNLIMITED)

        override suspend fun put(id: Long) {
            stored.send(id)
        }
    }

    private val clients = mutableListOf<NatsClientImpl>()
    private val streamsToPurge = mutableListOf<ServiceNames>()
    private var clientCounter = 0
    private val defaultBackoff = RpcDefaults.NAK_BACKOFF

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @AfterEach
    fun resetDefaults() {
        RpcDefaults.NAK_BACKOFF = defaultBackoff
    }

    // ---- persistent fire from external producers ----

    @Test
    fun `external protobuf fire is executed`() = test {
        val client = client()
        val impl = RolesImpl()
        client.registerService<RolesService>(impl)
        val streams = track(client.serviceDescriptor(RolesService::class.java))

        publishExternal(WHITELIST, whitelist("ADD", 42))

        assertEquals("ADD" to 42L, withTimeout(10.seconds) { impl.whitelisted.receive() })
        eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
    }

    @Test
    fun `undecodable external fire is dead-lettered without redelivery`() = test {
        RpcDefaults.NAK_BACKOFF = listOf(1.minutes) // a redelivery would not happen within the test
        val client = client()
        val impl = RolesImpl()
        client.registerService<RolesService>(impl)
        val descriptor = client.serviceDescriptor(RolesService::class.java)
        val streams = track(descriptor)
        val before = delivered(descriptor)

        publishExternal(WHITELIST, whitelist("ADD", 1), contentType = null)
        publishExternal(WHITELIST, whitelist("ADD", 2), contentType = "text/plain")
        publishExternal(WHITELIST, byteArrayOf(0x7f, 0x01, 0x02))

        eventually("all undecodable messages must be dead-lettered") { messageCount(streams.dlqStream) == 3L }
        eventually("work queue must be empty after term") { messageCount(streams.stream) == 0L }
        assertEquals(3L, delivered(descriptor) - before, "a decode failure must not be redelivered")
        assertNull(impl.whitelisted.tryReceive().getOrNull(), "the implementation must not be called")

        // The dead letters keep the original Content-Type next to the error
        val errors = deadLetters(streams).associate { dead ->
            val headers = assertNotNull(dead.headers)
            assertEquals(WHITELIST, headers.getFirst(ProtocolHeaders.DEAD_SUBJECT))
            headers.getFirst(ProtocolHeaders.CONTENT_TYPE) to assertNotNull(headers.getFirst(ProtocolHeaders.ERROR))
        }
        assertEquals(setOf(null, "text/plain", ProtocolHeaders.PROTOBUF_CONTENT_TYPE), errors.keys)
        assertEquals(IllegalArgumentException::class.java.name, errors[null])
        assertEquals(IllegalArgumentException::class.java.name, errors["text/plain"])
        val decodeError = Class.forName(errors.getValue(ProtocolHeaders.PROTOBUF_CONTENT_TYPE))
        assertTrue(SerializationException::class.java.isAssignableFrom(decodeError), "was $decodeError")
    }

    @Test
    fun `unknown persistent method is redelivered and then dead-lettered`() = test {
        RpcDefaults.NAK_BACKOFF = listOf(500.milliseconds)
        val client = client()
        client.registerService<RolesService>(RolesImpl())
        val descriptor = client.serviceDescriptor(RolesService::class.java)
        val streams = track(descriptor)
        val before = delivered(descriptor)

        publishExternal("surf.persist.surf.nats.test.v1.RolesService.Unknown", whitelist("ADD", 3))

        eventually("unknown method must end in the dead letter stream", timeout = 20.seconds) {
            messageCount(streams.dlqStream) == 1L
        }
        assertEquals(
            RpcDefaults.MAX_DELIVER.toLong(),
            delivered(descriptor) - before,
            "the message must be delivered MAX_DELIVER times before it is dead-lettered"
        )
        val deadHeaders = assertNotNull(jsm().getLastMessage(streams.dlqStream, streams.dlqSubject).headers)
        assertEquals(IllegalStateException::class.java.name, deadHeaders.getFirst(ProtocolHeaders.ERROR))
        eventually("work queue must be empty after term") { messageCount(streams.stream) == 0L }
    }

    @Test
    fun `unknown persistent method with a Nats-TTL expires before it is dead-lettered`() = test {
        RpcDefaults.NAK_BACKOFF = listOf(1.minutes) // a redelivery would not happen within the test
        val client = client()
        client.registerService<RolesService>(RolesImpl())
        val descriptor = client.serviceDescriptor(RolesService::class.java)
        val streams = track(descriptor)
        val before = delivered(descriptor)

        publishExternal(
            "surf.persist.surf.nats.test.v1.RolesService.Unknown",
            NatsCodec.encode(EchoRequest.serializer(), EchoRequest(4)),
            extraHeaders = mapOf(ProtocolHeaders.NATS_TTL to "2s"),
        )

        eventually("the NAKed call must expire with its Nats-TTL", timeout = 15.seconds) {
            messageCount(streams.stream) == 0L
        }
        assertEquals(1L, delivered(descriptor) - before, "the expired call must not be redelivered")
        assertEquals(0L, messageCount(streams.dlqStream), "the expired call must not be dead-lettered")
    }

    @Test
    fun `fire with a missing payload object is dead-lettered after one delivery`() = test {
        RpcDefaults.NAK_BACKOFF = listOf(1.minutes) // a redelivery would not happen within the test
        val client = client()
        val impl = RolesImpl()
        client.registerService<RolesService>(impl)
        val descriptor = client.serviceDescriptor(RolesService::class.java)
        val streams = track(descriptor)
        val before = delivered(descriptor)

        publishExternal(
            WHITELIST,
            NatsHeaders.EMPTY_BODY,
            extraHeaders = mapOf(ProtocolHeaders.PAYLOAD_REF to UUID.randomUUID().toString()),
        )

        eventually("a missing payload object must be dead-lettered without waiting for a redelivery") {
            messageCount(streams.dlqStream) == 1L
        }
        eventually("work queue must be empty after term") { messageCount(streams.stream) == 0L }
        assertEquals(1L, delivered(descriptor) - before, "a permanent failure must not be redelivered")
        assertNull(impl.whitelisted.tryReceive().getOrNull(), "the implementation must not be called")

        val deadHeaders = assertNotNull(jsm().getLastMessage(streams.dlqStream, streams.dlqSubject).headers)
        assertNotNull(deadHeaders.getFirst(ProtocolHeaders.ERROR))
        assertEquals(WHITELIST, deadHeaders.getFirst(ProtocolHeaders.DEAD_SUBJECT))
    }

    @Test
    fun `external publish before the first registration fails and works afterwards`() = test {
        val streams = deleteLateStreams()
        val subject = "surf.persist.surf.nats.test.v1.LateService.Put"
        assertFails { publishExternal(subject, NatsCodec.encode(PutRequest.serializer(), PutRequest(1))) }

        val client = client()
        val impl = LateImpl()
        client.registerService<LateService>(impl)
        track(client.serviceDescriptor(LateService::class.java))
        assertEquals("SURF_PERSIST_surf_nats_test_v1_LateService", streams.stream)

        publishExternal(subject, NatsCodec.encode(PutRequest.serializer(), PutRequest(2)))
        assertEquals(2L, withTimeout(10.seconds) { impl.stored.receive() })
    }

    // ---- shared services ----

    @Test
    fun `differently named clients share one service and its durable`() = test {
        val first = RolesImpl()
        val second = RolesImpl()
        client("roles-a").registerService<RolesService>(first)
        client("roles-b").registerService<RolesService>(second)
        val caller = client()
        val streams = track(caller.serviceDescriptor(RolesService::class.java))
        assertEquals(listOf(streams.durable), jsm().getConsumerNames(streams.stream))
        val roles = caller.service<RolesService>()
        val ids = (1L..20L).toList()

        for (id in ids) roles.store(id)
        eventually("every persistent call must be processed") { first.stored.size + second.stored.size >= ids.size }
        eventually("work queue must be drained and acked") {
            messageCount(streams.stream) == 0L &&
                    jsm().getConsumerInfo(streams.stream, streams.durable).numAckPending == 0L
        }

        for (id in ids) assertEquals(Number(id), roles.handle(id))

        assertEquals(ids, (first.stored + second.stored).sorted(), "each persistent call must run exactly once")
        assertEquals(ids, (first.handled + second.handled).sorted(), "each core call must run exactly once")
        assertEquals(0L, messageCount(streams.dlqStream))
    }

    @Test
    fun `differently named clients register one service concurrently`() = test {
        // Every registration flushes the shared connection; more than two concurrent flushes used to exceed
        // the outgoing ping limit of jnats and drop the connection
        repeat(5) { round ->
            val registrations = List(4) { client("roles-concurrent-$round-$it") }.map { client ->
                async { client.registerService<RolesService>(RolesImpl()) }
            }.awaitAll()
            registrations.forEach { it.close() }
        }

        client("roles-concurrent-a").registerService<RolesService>(RolesImpl())
        val caller = client()
        val streams = track(caller.serviceDescriptor(RolesService::class.java))
        assertEquals(listOf(streams.durable), jsm().getConsumerNames(streams.stream))
        assertEquals(Number(1), caller.service<RolesService>().handle(1))
    }

    @Test
    fun `concurrently closed registrations keep the shared connection`() = test {
        // Every close drains its dispatcher, which flushes the shared connection like a registration does
        val connection = NatsConnectionManager.connection()
        val reconnects = connection.statistics.reconnects
        repeat(10) { round ->
            val registrations = List(6) { client("roles-closing-$round-$it") }
                .map { it.registerService<RolesService>(RolesImpl()) }
            val barrier = CyclicBarrier(registrations.size)
            registrations.map { registration ->
                async(Dispatchers.IO) {
                    barrier.await()
                    registration.close()
                }
            }.awaitAll()
        }

        assertEquals(reconnects, connection.statistics.reconnects, "the shared connection must not reconnect")
        client("roles-closing-a").registerService<RolesService>(RolesImpl())
        val caller = client()
        val streams = track(caller.serviceDescriptor(RolesService::class.java))
        assertEquals(listOf(streams.durable), jsm().getConsumerNames(streams.stream))
        assertEquals(Number(1), caller.service<RolesService>().handle(1))
    }

    // ---- core requests from external callers ----

    @Test
    fun `core request without content type is answered with an error`() = test {
        val client = client()
        client.registerService<RolesService>(RolesImpl())
        track(client.serviceDescriptor(RolesService::class.java))

        val reply = requestReply(ECHO, null, NatsCodec.encode(EchoRequest.serializer(), EchoRequest(5)))

        assertEquals(IllegalArgumentException::class.java.name, reply.headers.getFirst(ProtocolHeaders.ERROR))
        assertContains(reply.data.decodeToString(), "Missing Content-Type")
    }

    @Test
    fun `core protobuf request is answered in protobuf`() = test {
        val client = client()
        client.registerService<RolesService>(RolesImpl())
        track(client.serviceDescriptor(RolesService::class.java))

        val reply = requestReply(ECHO, protobufHeaders(), NatsCodec.encode(EchoRequest.serializer(), EchoRequest(5)))

        assertNull(reply.headers.getFirst(ProtocolHeaders.ERROR), reply.data.decodeToString())
        val contentType = reply.headers.getFirst(ProtocolHeaders.CONTENT_TYPE)
        assertEquals(ProtocolHeaders.PROTOBUF_CONTENT_TYPE, contentType)
        assertContentEquals(byteArrayOf(0x08, 0x05), reply.data, "Number { value: 5 }")
        assertEquals(Number(5), NatsCodec.decode(Number.serializer(), reply.data, contentType))
    }

    @Test
    fun `UUIDs are 16-byte messages for external callers and still work for Kotlin callers`() = test {
        val client = client()
        client.registerService<RolesService>(RolesImpl())
        track(client.serviceDescriptor(RolesService::class.java))
        val uuid = UUID.randomUUID()

        // OwnerRequest { player: Uuid { value: <msb><lsb> } }, field 1 of both messages
        val bytes = ByteBuffer.allocate(16).putLong(uuid.mostSignificantBits).putLong(uuid.leastSignificantBits).array()
        val encoded = byteArrayOf(0x0a, 18, 0x0a, 16) + bytes
        val reply = requestReply("surf.rpc.surf.nats.test.v1.RolesService.Owner", protobufHeaders(), encoded)

        assertNull(reply.headers.getFirst(ProtocolHeaders.ERROR), reply.data.decodeToString())
        assertContentEquals(encoded, reply.data, "OwnerReply has the same layout")
        assertEquals(OwnerReply(uuid), NatsCodec.decode(OwnerReply.serializer(), reply.data, ProtocolHeaders.PROTOBUF_CONTENT_TYPE))

        assertEquals(OwnerReply(uuid), client.service<RolesService>().owner(uuid))
    }

    // ---- foreign streams ----

    @Test
    fun `stream created for other subjects is not rewritten`() = test {
        val streams = deleteLateStreams()

        // Created by another tool under the name surf-nats derives for LateService
        val foreign = listOf("other.persist.LateService.>")
        jsm().addStream(
            StreamConfiguration.builder()
                .name(streams.stream)
                .subjects(foreign)
                .storageType(StorageType.File)
                .build()
        )

        try {
            val error = assertFailsWith<IllegalStateException> {
                client().registerService<LateService>(LateImpl())
            }
            assertContains(error.message.orEmpty(), streams.stream)
            assertEquals(foreign, jsm().getStreamInfo(streams.stream).configuration.subjects)
        } finally {
            deleteLateStreams()
        }
    }

    // ---- helpers ----

    private fun client(name: String = "ext-${clientCounter++}-${UUID.randomUUID().toString().take(6)}") =
        (NatsClient.create(name) as NatsClientImpl).also { clients += it }

    private fun track(descriptor: ServiceDescriptor): ServiceNames =
        descriptor.streams!!.also { streamsToPurge += it }

    private fun delivered(descriptor: ServiceDescriptor): Long {
        val streams = descriptor.streams!!
        return jsm().getConsumerInfo(streams.stream, streams.durable).delivered.consumerSequence
    }

    // The streams of LateService are deleted, unlike the purged streams of the other services, so a test can rely on
    // their absence; the provisioning cache forgets them
    private fun deleteLateStreams(): ServiceNames {
        val streams = ServiceDescriptor.of(LateService::class.java).streams!!
        for (stream in listOf(streams.stream, streams.dlqStream)) runCatching { jsm().deleteStream(stream) }
        JetStreamProvisioner.invalidate(streams)
        return streams
    }

    private fun deadLetters(streams: ServiceNames) = jsm().getStreamInfo(streams.dlqStream).streamState
        .let { state -> (state.firstSequence..state.lastSequence) }
        .map { jsm().getMessage(streams.dlqStream, it) }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runTest(timeout = 2.minutes) {
        try {
            withContext(Dispatchers.Default) { block() }
        } finally {
            clients.forEach { it.close() }
            clients.clear()
            // Purged instead of deleted: the streams of a service are shared by all tests, and the provisioning
            // cache would not notice a deleted stream
            streamsToPurge.forEach { purgeStreams(it.stream, it.dlqStream) }
            streamsToPurge.clear()
        }
    }

    private fun whitelist(action: String, userId: Long) =
        NatsCodec.encode(WhitelistRequest.serializer(), WhitelistRequest(action, userId))

    private fun protobufHeaders() = Headers().add(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)

    // Raw jnats producer: a Nats-Msg-Id and an optional Content-Type, no Surf headers
    private fun publishExternal(
        subject: String,
        body: ByteArray,
        contentType: String? = ProtocolHeaders.PROTOBUF_CONTENT_TYPE,
        extraHeaders: Map<String, String> = emptyMap(),
    ) {
        val headers = Headers().add(NatsJetStreamConstants.MSG_ID_HDR, UUID.randomUUID().toString())
        if (contentType != null) headers.add(ProtocolHeaders.CONTENT_TYPE, contentType)
        extraHeaders.forEach { (name, value) -> headers.add(name, value) }
        NatsConnectionManager.connection().jetStream().publish(subject, headers, body)
    }

    private fun purgeStreams(vararg streams: String) {
        for (stream in streams) runCatching { jsm().purgeStream(stream) }
    }

    companion object {
        private const val WHITELIST = "surf.persist.surf.nats.test.v1.RolesService.Whitelist"
        private const val ECHO = "surf.rpc.surf.nats.test.v1.RolesService.Echo"
    }
}
