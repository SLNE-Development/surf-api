package dev.slne.surf.nats.rpc

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.jetstream.PersistentStreams
import io.nats.client.api.StorageType
import io.nats.client.api.StreamConfiguration
import io.nats.client.impl.Headers
import io.nats.client.support.NatsJetStreamConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExternalCallerTest {

    @NatsService("ext-roles", prefixed = false)
    interface RolesService {
        @NatsPersistent
        @NatsMethod("whitelist")
        suspend fun whitelist(action: String, @NatsParam("userId") userId: Long)

        suspend fun echo(id: Long): Long

        suspend fun owner(id: UUID): UUID
    }

    class RolesImpl : RolesService {
        val whitelisted = Channel<Pair<String, Long>>(Channel.UNLIMITED)

        override suspend fun whitelist(action: String, userId: Long) {
            whitelisted.send(action to userId)
        }

        override suspend fun echo(id: Long): Long = id

        override suspend fun owner(id: UUID): UUID = id
    }

    @NatsService("ext-late", prefixed = false)
    interface LateService {
        @NatsPersistent
        suspend fun store(id: Long)
    }

    class LateImpl : LateService {
        val stored = Channel<Long>(Channel.UNLIMITED)

        override suspend fun store(id: Long) {
            stored.send(id)
        }
    }

    @NatsService("ext-shared", prefixed = false)
    interface SharedService {
        @NatsPersistent
        suspend fun store(id: Long)

        suspend fun handle(id: Long): Long
    }

    class SharedImpl : SharedService {
        val stored = ConcurrentLinkedQueue<Long>()
        val handled = ConcurrentLinkedQueue<Long>()

        override suspend fun store(id: Long) {
            stored += id
        }

        override suspend fun handle(id: Long): Long {
            handled += id
            return id
        }
    }

    @NatsService("x-y")
    interface CollidingService {
        @NatsPersistent
        suspend fun put(id: Long)
    }

    @NatsService("y")
    interface OtherCollidingService {
        @NatsPersistent
        suspend fun put(id: Long)
    }

    class CollidingImpl : CollidingService, OtherCollidingService {
        override suspend fun put(id: Long) = Unit
    }

    @NatsService("inner")
    interface CoexInnerService {
        @NatsPersistent
        suspend fun put(id: Long)
    }

    @NatsService("coex", prefixed = false)
    interface CoexOuterService {
        @NatsPersistent
        suspend fun put(id: Long)
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
    private val streamsToPurge = mutableListOf<PersistentStreams>()
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
    fun `external JSON fire is executed`() = test {
        val client = client()
        val impl = RolesImpl()
        client.registerService<RolesService>(impl)
        val streams = track(client.serviceDescriptor(RolesService::class.java))

        publishExternal(WHITELIST, """{"action":"ADD","user_id":42}""")

        assertEquals("ADD" to 42L, withTimeout(10.seconds) { impl.whitelisted.receive() })
        eventually("work queue must be empty after the ack") { messageCount(streams.stream) == 0L }
    }

    @Test
    fun `undecodable external fire is dead-lettered without redelivery`() = test {
        RpcDefaults.NAK_BACKOFF = listOf(1.minutes) // a redelivery would not happen within the test
        val client = client()
        val impl = RolesImpl()
        client.registerService<RolesService>(impl)
        val streams = track(client.serviceDescriptor(RolesService::class.java))

        publishExternal(WHITELIST, """{"action":"ADD","user_id":1}""", contentType = null)
        publishExternal(WHITELIST, """{"action":"ADD","user_id":2}""", contentType = "text/plain")
        publishExternal(WHITELIST, "not json")

        eventually("all undecodable messages must be dead-lettered") { messageCount(streams.dlqStream) == 3L }
        eventually("work queue must be empty after term") { messageCount(streams.stream) == 0L }
        assertNull(impl.whitelisted.tryReceive().getOrNull(), "the implementation must not be called")

        val deadHeaders = assertNotNull(jsm().getLastMessage(streams.dlqStream, streams.dlqSubject).headers)
        assertNotNull(deadHeaders.getFirst(NatsHeaders.ERROR))
        assertEquals(WHITELIST, deadHeaders.getFirst(NatsHeaders.DEAD_SUBJECT))
    }

    @Test
    fun `unknown persistent method is redelivered and then dead-lettered`() = test {
        RpcDefaults.NAK_BACKOFF = listOf(500.milliseconds)
        val client = client()
        client.registerService<RolesService>(RolesImpl())
        val descriptor = client.serviceDescriptor(RolesService::class.java)
        val streams = track(descriptor)
        fun delivered() =
            jsm().getConsumerInfo(streams.stream, descriptor.responderGroup).delivered.consumerSequence
        val before = delivered()

        publishExternal("ext-roles.\$persist.unknown", "{}")

        eventually("unknown method must end in the dead letter stream", timeout = 20.seconds) {
            messageCount(streams.dlqStream) == 1L
        }
        assertEquals(
            RpcDefaults.MAX_DELIVER.toLong(),
            delivered() - before,
            "the message must be delivered MAX_DELIVER times before it is dead-lettered"
        )
        val deadHeaders = assertNotNull(jsm().getLastMessage(streams.dlqStream, streams.dlqSubject).headers)
        assertEquals(IllegalStateException::class.java.name, deadHeaders.getFirst(NatsHeaders.ERROR))
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
        fun delivered() =
            jsm().getConsumerInfo(streams.stream, descriptor.responderGroup).delivered.consumerSequence
        val before = delivered()

        publishExternal(
            WHITELIST,
            "",
            extraHeaders = mapOf(NatsHeaders.PAYLOAD_REF to UUID.randomUUID().toString()),
        )

        eventually("a missing payload object must be dead-lettered without waiting for a redelivery") {
            messageCount(streams.dlqStream) == 1L
        }
        eventually("work queue must be empty after term") { messageCount(streams.stream) == 0L }
        assertEquals(1L, delivered() - before, "a permanent failure must not be redelivered")
        assertNull(impl.whitelisted.tryReceive().getOrNull(), "the implementation must not be called")

        val deadHeaders = assertNotNull(jsm().getLastMessage(streams.dlqStream, streams.dlqSubject).headers)
        assertNotNull(deadHeaders.getFirst(NatsHeaders.ERROR))
        assertEquals(WHITELIST, deadHeaders.getFirst(NatsHeaders.DEAD_SUBJECT))
    }

    @Test
    fun `external publish before the first registration fails and works afterwards`() = test {
        val subject = "ext-late.\$persist.store"
        assertFails { publishExternal(subject, """{"id":1}""") }

        val client = client()
        val impl = LateImpl()
        client.registerService<LateService>(impl)
        track(client.serviceDescriptor(LateService::class.java))

        publishExternal(subject, """{"id":2}""")
        assertEquals(2L, withTimeout(10.seconds) { impl.stored.receive() })
    }

    // ---- unprefixed services ----

    @Test
    fun `unprefixed service is shared between differently named clients`() = test {
        val first = SharedImpl()
        val second = SharedImpl()
        client().registerService<SharedService>(first)
        client().registerService<SharedService>(second)
        val caller = client()
        val descriptor = caller.serviceDescriptor(SharedService::class.java)
        val streams = track(descriptor)
        val shared = caller.service<SharedService>()
        val ids = (1L..20L).toList()

        for (id in ids) shared.store(id)
        eventually("every persistent call must be processed") { first.stored.size + second.stored.size >= ids.size }
        eventually("work queue must be drained and acked") {
            messageCount(streams.stream) == 0L &&
                    jsm().getConsumerInfo(streams.stream, descriptor.responderGroup).numAckPending == 0L
        }

        for (id in ids) assertEquals(id, shared.handle(id))

        assertEquals(ids, (first.stored + second.stored).sorted(), "each persistent call must run exactly once")
        assertEquals(ids, (first.handled + second.handled).sorted(), "each core call must run exactly once")
    }

    @Test
    fun `prefixed and unprefixed persistent services of the same name coexist`() = test {
        val client = client("coex")
        val inner = CoexInnerImpl()
        val outer = CoexOuterImpl()
        val innerStreams = track(client.serviceDescriptor(CoexInnerService::class.java))
        val outerStreams = track(client.serviceDescriptor(CoexOuterService::class.java))
        assertEquals("coex.\$persist.inner.*", innerStreams.filter)
        assertEquals("coex.\$persist.*", outerStreams.filter)

        client.registerService<CoexInnerService>(inner)
        client.registerService<CoexOuterService>(outer)

        assertEquals(listOf(innerStreams.filter), jsm().getStreamInfo(innerStreams.stream).configuration.subjects)
        assertEquals(listOf(outerStreams.filter), jsm().getStreamInfo(outerStreams.stream).configuration.subjects)

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

        val reply = assertNotNull(
            NatsConnectionManager.connection()
                .request("ext-roles.\$rpc.echo", """{"id":5}""".encodeToByteArray(), java.time.Duration.ofSeconds(5)),
            "no reply"
        )

        assertEquals(IllegalArgumentException::class.java.name, reply.headers.getFirst(NatsHeaders.ERROR))
        assertContains(reply.data.decodeToString(), "Missing Content-Type")
    }

    @Test
    fun `core JSON request is answered in JSON`() = test {
        val client = client()
        client.registerService<RolesService>(RolesImpl())
        track(client.serviceDescriptor(RolesService::class.java))

        val headers = Headers().add(NatsHeaders.CONTENT_TYPE, WireFormat.JSON.contentType)
        val reply = assertNotNull(
            NatsConnectionManager.connection().request(
                "ext-roles.\$rpc.echo",
                headers,
                """{"id":5}""".encodeToByteArray(),
                java.time.Duration.ofSeconds(5)
            ),
            "no reply"
        )

        assertNull(reply.headers.getFirst(NatsHeaders.ERROR))
        assertEquals(WireFormat.JSON.contentType, reply.headers.getFirst(NatsHeaders.CONTENT_TYPE))
        assertEquals("5", reply.data.decodeToString())
    }

    @Test
    fun `UUIDs are strings for JSON callers and still work for Kotlin callers`() = test {
        val client = client()
        client.registerService<RolesService>(RolesImpl())
        track(client.serviceDescriptor(RolesService::class.java))
        val uuid = UUID.randomUUID()

        val headers = Headers().add(NatsHeaders.CONTENT_TYPE, WireFormat.JSON.contentType)
        val reply = assertNotNull(
            NatsConnectionManager.connection().request(
                "ext-roles.\$rpc.owner",
                headers,
                """{"id":"$uuid"}""".encodeToByteArray(),
                java.time.Duration.ofSeconds(5)
            ),
            "no reply"
        )
        assertNull(reply.headers.getFirst(NatsHeaders.ERROR), reply.data.decodeToString())
        assertEquals("\"$uuid\"", reply.data.decodeToString())

        assertEquals(uuid, client.service<RolesService>().owner(uuid))
    }

    // ---- stream name collisions ----

    @Test
    fun `stream owned by another service is not rewritten`() = test {
        val client = client("coll")
        val streams = track(client.serviceDescriptor(CollidingService::class.java))

        // Created by "coll-x" for its service "y", e.g. from another JVM
        val foreign = listOf("coll-x.\$persist.y.>")
        jsm().addStream(
            StreamConfiguration.builder()
                .name(streams.stream)
                .subjects(foreign)
                .storageType(StorageType.File)
                .build()
        )

        val error = assertFailsWith<IllegalStateException> {
            client.registerService<CollidingService>(CollidingImpl())
        }
        assertContains(error.message.orEmpty(), streams.stream)
        assertEquals(foreign, jsm().getStreamInfo(streams.stream).configuration.subjects)
    }

    @Test
    fun `stream name collision inside one JVM is rejected`() = test {
        val owner = client("coll2-x")
        owner.registerService<OtherCollidingService>(CollidingImpl())
        val streams = track(owner.serviceDescriptor(OtherCollidingService::class.java))

        val error = assertFailsWith<IllegalStateException> {
            client("coll2").registerService<CollidingService>(CollidingImpl())
        }
        assertContains(error.message.orEmpty(), streams.stream)
        assertEquals(listOf(streams.filter), jsm().getStreamInfo(streams.stream).configuration.subjects)
    }

    // ---- helpers ----

    private fun client(name: String = "ext-${clientCounter++}-${UUID.randomUUID().toString().take(6)}") =
        (NatsClient.create(name) as NatsClientImpl).also { clients += it }

    private fun track(descriptor: ServiceDescriptor): PersistentStreams =
        descriptor.streams!!.also { streamsToPurge += it }

    private fun test(block: suspend CoroutineScope.() -> Unit) = runTest(timeout = 2.minutes) {
        try {
            withContext(Dispatchers.Default) { block() }
        } finally {
            clients.forEach { it.close() }
            clients.clear()
            // Purged instead of deleted: unprefixed streams are shared by tests, and the provisioning cache
            // would not notice a deleted stream
            streamsToPurge.forEach { purgeStreams(it.stream, it.dlqStream) }
            streamsToPurge.clear()
        }
    }

    private fun publishExternal(
        subject: String,
        body: String,
        contentType: String? = WireFormat.JSON.contentType,
        extraHeaders: Map<String, String> = emptyMap(),
    ) {
        val headers = Headers().add(NatsJetStreamConstants.MSG_ID_HDR, UUID.randomUUID().toString())
        if (contentType != null) headers.add(NatsHeaders.CONTENT_TYPE, contentType)
        extraHeaders.forEach { (name, value) -> headers.add(name, value) }
        NatsConnectionManager.connection().jetStream().publish(subject, headers, body.encodeToByteArray())
    }

    private fun jsm() = NatsConnectionManager.connection().jetStreamManagement()

    private fun messageCount(stream: String): Long =
        jsm().getStreamInfo(stream).streamState.msgCount

    private fun purgeStreams(vararg streams: String) {
        for (stream in streams) runCatching { jsm().purgeStream(stream) }
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
        private const val WHITELIST = "ext-roles.\$persist.whitelist"
    }
}
