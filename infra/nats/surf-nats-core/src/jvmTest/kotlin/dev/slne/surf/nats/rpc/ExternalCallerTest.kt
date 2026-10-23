package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.protocol.ServiceNames
import dev.slne.surf.nats.test.proto.v1.CoexInnerService
import dev.slne.surf.nats.test.proto.v1.CoexOuterService
import dev.slne.surf.nats.test.proto.v1.CollidingXyService
import dev.slne.surf.nats.test.proto.v1.CollidingYService
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

    class CollidingImpl : CollidingXyService, CollidingYService {
        override suspend fun put(id: Long) = Unit
    }

    class CoexInnerImpl : CoexInnerService {
        val stored = ConcurrentLinkedQueue<Long>()

        override suspend fun put(id: Long) {
            stored += id
        }
    }

    class CoexOuterImpl : CoexOuterService {
        val stored = ConcurrentLinkedQueue<Long>()

        override suspend fun put(id: Long) {
            stored += id
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

        publishExternal("ext-roles.\$persist.v1.unknown", whitelist("ADD", 3))

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
        val subject = "ext-late.\$persist.v1.put"
        assertFails { publishExternal(subject, NatsCodec.encode(PutRequest.serializer(), PutRequest(1))) }

        val client = client()
        val impl = LateImpl()
        client.registerService<LateService>(impl)
        val streams = track(client.serviceDescriptor(LateService::class.java))
        assertEquals("SURF__EXT_LATE_V1", streams.stream)

        publishExternal(subject, NatsCodec.encode(PutRequest.serializer(), PutRequest(2)))
        assertEquals(2L, withTimeout(10.seconds) { impl.stored.receive() })
    }

    // ---- global services ----

    @Test
    fun `global service is shared between differently named clients`() = test {
        val first = RolesImpl()
        val second = RolesImpl()
        client().registerService<RolesService>(first)
        client().registerService<RolesService>(second)
        val caller = client()
        val descriptor = caller.serviceDescriptor(RolesService::class.java)
        val streams = track(descriptor)
        val roles = caller.service<RolesService>()
        val ids = (1L..20L).toList()

        for (id in ids) roles.store(id)
        eventually("every persistent call must be processed") { first.stored.size + second.stored.size >= ids.size }
        eventually("work queue must be drained and acked") {
            messageCount(streams.stream) == 0L &&
                    jsm().getConsumerInfo(streams.stream, descriptor.responderGroup).numAckPending == 0L
        }

        for (id in ids) assertEquals(Number(id), roles.handle(id))

        assertEquals(ids, (first.stored + second.stored).sorted(), "each persistent call must run exactly once")
        assertEquals(ids, (first.handled + second.handled).sorted(), "each core call must run exactly once")
    }

    @Test
    fun `prefixed and global persistent services of the same name coexist`() = test {
        val client = client("coex")
        val inner = CoexInnerImpl()
        val outer = CoexOuterImpl()
        val innerStreams = track(client.serviceDescriptor(CoexInnerService::class.java))
        val outerStreams = track(client.serviceDescriptor(CoexOuterService::class.java))
        assertEquals("coex.\$persist.v1.inner.*", innerStreams.streamFilter)
        assertEquals("coex.\$persist.v1.*", outerStreams.streamFilter)

        client.registerService<CoexInnerService>(inner)
        client.registerService<CoexOuterService>(outer)

        assertEquals(listOf(innerStreams.streamFilter), jsm().getStreamInfo(innerStreams.stream).configuration.subjects)
        assertEquals(listOf(outerStreams.streamFilter), jsm().getStreamInfo(outerStreams.stream).configuration.subjects)

        client.service<CoexInnerService>().put(1)
        client.service<CoexOuterService>().put(2)

        eventually("both calls must be processed") { inner.stored.size == 1 && outer.stored.size == 1 }
        eventually("both work queues must be drained") {
            messageCount(innerStreams.stream) == 0L && messageCount(outerStreams.stream) == 0L
        }
        assertEquals(listOf(1L), inner.stored.toList())
        assertEquals(listOf(2L), outer.stored.toList())
        assertEquals(0L, messageCount(innerStreams.dlqStream))
        assertEquals(0L, messageCount(outerStreams.dlqStream))
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
        val reply = requestReply("ext-roles.\$rpc.v1.owner", protobufHeaders(), encoded)

        assertNull(reply.headers.getFirst(ProtocolHeaders.ERROR), reply.data.decodeToString())
        assertContentEquals(encoded, reply.data, "OwnerReply has the same layout")
        assertEquals(OwnerReply(uuid), NatsCodec.decode(OwnerReply.serializer(), reply.data, ProtocolHeaders.PROTOBUF_CONTENT_TYPE))

        assertEquals(OwnerReply(uuid), client.service<RolesService>().owner(uuid))
    }

    // ---- stream name collisions ----

    @Test
    fun `stream owned by another service is not rewritten`() = test {
        val client = client("coll")
        val streams = track(client.serviceDescriptor(CollidingXyService::class.java))
        assertEquals("SURF_COLL_X_Y_V1", streams.stream)

        // Created by "coll-x" for its service "y", e.g. from another JVM
        val foreign = listOf("coll-x.\$persist.v1.y.>")
        jsm().addStream(
            StreamConfiguration.builder()
                .name(streams.stream)
                .subjects(foreign)
                .storageType(StorageType.File)
                .build()
        )

        val error = assertFailsWith<IllegalStateException> {
            client.registerService<CollidingXyService>(CollidingImpl())
        }
        assertContains(error.message.orEmpty(), streams.stream)
        assertEquals(foreign, jsm().getStreamInfo(streams.stream).configuration.subjects)
    }

    @Test
    fun `stream name collision inside one JVM is rejected`() = test {
        val owner = client("coll2-x")
        owner.registerService<CollidingYService>(CollidingImpl())
        val streams = track(owner.serviceDescriptor(CollidingYService::class.java))
        assertEquals("SURF_COLL2_X_Y_V1", streams.stream)

        val error = assertFailsWith<IllegalStateException> {
            client("coll2").registerService<CollidingXyService>(CollidingImpl())
        }
        assertContains(error.message.orEmpty(), streams.stream)
        assertEquals(listOf(streams.streamFilter), jsm().getStreamInfo(streams.stream).configuration.subjects)
    }

    // ---- helpers ----

    private fun client(name: String = "ext-${clientCounter++}-${UUID.randomUUID().toString().take(6)}") =
        (NatsClient.create(name) as NatsClientImpl).also { clients += it }

    private fun track(descriptor: ServiceDescriptor): ServiceNames =
        descriptor.streams!!.also { streamsToPurge += it }

    private fun delivered(descriptor: ServiceDescriptor): Long =
        jsm().getConsumerInfo(descriptor.streams!!.stream, descriptor.responderGroup).delivered.consumerSequence

    private fun deadLetters(streams: ServiceNames) = jsm().getStreamInfo(streams.dlqStream).streamState
        .let { state -> (state.firstSequence..state.lastSequence) }
        .map { jsm().getMessage(streams.dlqStream, it) }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runTest(timeout = 2.minutes) {
        try {
            withContext(Dispatchers.Default) { block() }
        } finally {
            clients.forEach { it.close() }
            clients.clear()
            // Purged instead of deleted: global streams are shared by tests, and the provisioning cache
            // would not notice a deleted stream
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
        private const val WHITELIST = "ext-roles.\$persist.v1.whitelist"
        private const val ECHO = "ext-roles.\$rpc.v1.echo"
    }
}
