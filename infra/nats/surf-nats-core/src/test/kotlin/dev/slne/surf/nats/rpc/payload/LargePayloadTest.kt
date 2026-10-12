package dev.slne.surf.nats.rpc.payload

import dev.slne.surf.nats.*
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.rpc.Answer
import dev.slne.surf.nats.rpc.NatsRecipients
import dev.slne.surf.nats.rpc.NatsService
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.rpc.withNatsCallTimeout
import io.nats.client.api.ObjectStoreConfiguration
import io.nats.client.api.StorageType
import io.nats.client.impl.Headers
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.ByteArraySerializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LargePayloadTest {

    @NatsService("blobs")
    interface BlobService {
        suspend fun checksum(data: ByteArray): Long
        suspend fun create(size: Int): ByteArray
        suspend fun echo(data: ByteArray): ByteArray
    }

    class BlobImpl : BlobService {
        val calls = AtomicInteger()

        override suspend fun checksum(data: ByteArray): Long {
            calls.incrementAndGet()
            return data.checksum()
        }

        override suspend fun create(size: Int): ByteArray = blob(size)

        override suspend fun echo(data: ByteArray): ByteArray = data
    }

    @NatsService("blob-fanout")
    @NatsRecipients(Recipients.ALL_INSTANCES)
    interface BlobFanout {
        suspend fun push(data: ByteArray)
        suspend fun find(size: Int): Answer<ByteArray>
    }

    class FanoutImpl : BlobFanout {
        val received = Channel<ByteArray>(Channel.UNLIMITED)

        override suspend fun push(data: ByteArray) {
            received.send(data)
        }

        override suspend fun find(size: Int): Answer<ByteArray> = Answer.Known(blob(size))
    }

    internal class Fixture(val client: NatsClientImpl, val blobs: BlobService, val impl: BlobImpl)

    private var clientCounter = 0

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @Test
    fun `large argument roundtrips and the request object is deleted`() = withService {
        val data = blob(LARGE)
        val before = liveObjects()

        assertEquals(data.checksum(), blobs.checksum(data))
        assertEquals(before, liveObjects(), "request object must be deleted by the responder")
    }

    @Test
    fun `large result roundtrips and the reply object is deleted`() = withService {
        val before = liveObjects()

        assertContentEquals(blob(LARGE), blobs.create(LARGE))
        assertEquals(before, liveObjects(), "reply object must be deleted by the caller")
    }

    @Test
    fun `large argument and large result in one call`() = withService {
        val data = blob(LARGE)
        val before = liveObjects()

        assertContentEquals(data, blobs.echo(data))
        assertEquals(before, liveObjects())
    }

    @Test
    fun `small payloads are sent inline`() = withService {
        val method = method("echo")
        val headers = NatsHeaders.base("external", WireFormat.CBOR)
        val body = client.codec.encode(method.arguments, arrayOf(blob(1_000)), WireFormat.CBOR)

        val reply = requestReply(method.subject, headers, body)

        assertNull(reply.headers.getFirst(NatsHeaders.PAYLOAD_REF))
        val contentType = reply.headers.getFirst(NatsHeaders.CONTENT_TYPE)
        assertContentEquals(blob(1_000), client.codec.decode(ByteArraySerializer(), reply.data, contentType))
    }

    @Test
    fun `offloaded reply carries only a reference`() = withService {
        val method = method("create")
        val headers = NatsHeaders.base("external", WireFormat.CBOR)
        val body = client.codec.encode(method.arguments, arrayOf(LARGE), WireFormat.CBOR)

        val reply = requestReply(method.subject, headers, body, Duration.ofSeconds(30))

        assertNull(reply.headers.getFirst(NatsHeaders.ERROR), reply.data.decodeToString())
        assertNotNull(reply.headers.getFirst(NatsHeaders.PAYLOAD_REF))
        assertEquals(0, reply.data.size)

        val data = PayloadStore.resolve(reply, PayloadBucket.TRANSIENT, delete = true)
        val contentType = reply.headers.getFirst(NatsHeaders.CONTENT_TYPE)
        assertContentEquals(blob(LARGE), client.codec.decode(ByteArraySerializer(), data, contentType))
    }

    @Test
    fun `json request gets an offloaded json reply`() = withService {
        val size = 2_000_000
        val headers = Headers().add(NatsHeaders.CONTENT_TYPE, WireFormat.JSON.contentType)
        val body = """{"size":$size}""".encodeToByteArray()

        val reply = requestReply(method("create").subject, headers, body, Duration.ofSeconds(30))

        assertNull(reply.headers.getFirst(NatsHeaders.ERROR), reply.data.decodeToString())
        assertNotNull(reply.headers.getFirst(NatsHeaders.PAYLOAD_REF))
        val contentType = assertNotNull(reply.headers.getFirst(NatsHeaders.CONTENT_TYPE))
        assertEquals(WireFormat.JSON, WireFormat.fromContentType(contentType))

        val data = PayloadStore.resolve(reply, PayloadBucket.TRANSIENT, delete = true)
        assertContentEquals(blob(size), client.codec.decode(ByteArraySerializer(), data, contentType))
    }

    @Test
    fun `fan-out payload is shared by all receivers and not deleted`() = withService {
        val impl = FanoutImpl()
        client.registerService<BlobFanout>(impl)
        val method = client.serviceDescriptor(BlobFanout::class.java).methods.single { it.method.name == "push" }

        val connection = NatsConnectionManager.connection()
        val other = connection.subscribe(method.subject)
        connection.flush(Duration.ofSeconds(5))

        val data = blob(LARGE)
        val before = liveObjects()
        try {
            client.service<BlobFanout>().push(data)

            assertContentEquals(data, withTimeout(30.seconds) { impl.received.receive() })

            val message = assertNotNull(other.nextMessage(Duration.ofSeconds(30)), "other receiver got nothing")
            val name = assertNotNull(message.headers.getFirst(NatsHeaders.PAYLOAD_REF))
            val arguments = client.codec.decode(
                method.arguments,
                PayloadStore.resolve(message, PayloadBucket.TRANSIENT, delete = false),
                message.headers.getFirst(NatsHeaders.CONTENT_TYPE)
            )
            assertContentEquals(data, arguments[0] as ByteArray)

            assertEquals(before + name, liveObjects(), "fan-out object must stay until the bucket TTL")
            store().delete(name)
        } finally {
            other.unsubscribe()
        }
    }

    @Test
    fun `large ALL_INSTANCES answer is resolved and deleted`() = withService {
        client.registerService<BlobFanout>(FanoutImpl())
        val before = liveObjects()

        val answer = withNatsCallTimeout(30.seconds) { client.service<BlobFanout>().find(LARGE) }

        val value = assertIs<Answer.Known<ByteArray>>(answer).value
        assertContentEquals(blob(LARGE), value)
        assertEquals(before, liveObjects())
    }

    @Test
    fun `timeout above the payload TTL fails before sending`() = withService {
        val before = liveObjects()
        val tooLong = PayloadBucket.TRANSIENT.ttl + 1.minutes

        assertFailsWith<IllegalArgumentException> {
            withNatsCallTimeout(tooLong) { blobs.checksum(blob(LARGE)) }
        }
        assertEquals(0, impl.calls.get())
        assertEquals(before, liveObjects())

        val small = blob(1_000)
        assertEquals(small.checksum(), withNatsCallTimeout(tooLong) { blobs.checksum(small) })
    }

    @Test
    fun `parallel large calls all succeed`() = withService {
        val before = liveObjects()

        val sums = coroutineScope {
            (1..5).map { i -> async { blobs.checksum(blob(LARGE + i)) } }.awaitAll()
        }

        assertEquals((1..5).map { blob(LARGE + it).checksum() }, sums)
        assertEquals(before, liveObjects())
    }

    @Test
    fun `bucket is created with the payload TTL`() = withService {
        blobs.checksum(blob(LARGE))

        val status = NatsConnectionManager.connection().objectStoreManagement().getStatus(PayloadBucket.TRANSIENT.bucketName)
        assertEquals(PayloadBucket.TRANSIENT.ttl.inWholeSeconds, status.ttl?.toSeconds())
        assertEquals(StorageType.File, status.storageType)
    }

    @Test
    fun `existing bucket with a different configuration is reused`() = runTest(timeout = 2.minutes) {
        withContext(Dispatchers.Default) {
            val ttl = Duration.ofMinutes(1)
            recreateBucket(ObjectStoreConfiguration.builder(PayloadBucket.TRANSIENT.bucketName).ttl(ttl).build())
            try {
                (NatsClient.create("rpc-payload-${clientCounter++}") as NatsClientImpl).use { client ->
                    val impl = BlobImpl()
                    client.registerService<BlobService>(impl)
                    val data = blob(LARGE)

                    assertEquals(data.checksum(), client.service<BlobService>().checksum(data))
                }

                val status = NatsConnectionManager.connection().objectStoreManagement()
                    .getStatus(PayloadBucket.TRANSIENT.bucketName)
                assertEquals(ttl, status.ttl, "existing bucket must not be replaced")
            } finally {
                recreateBucket(null)
            }
        }
    }

    private suspend fun recreateBucket(config: ObjectStoreConfiguration?) {
        val management = NatsConnectionManager.connection().objectStoreManagement()
        withContext(Dispatchers.IO) {
            if (PayloadBucket.TRANSIENT.bucketName in management.bucketNames) management.delete(PayloadBucket.TRANSIENT.bucketName)
            if (config != null) management.create(config)
        }
        NatsInstance.instance.disable()
        NatsInstance.instance.load()
    }

    private fun withService(block: suspend Fixture.() -> Unit) = runTest(timeout = 2.minutes) {
        (NatsClient.create("rpc-payload-${clientCounter++}") as NatsClientImpl).use { client ->
            val impl = BlobImpl()
            client.registerService<BlobService>(impl)
            val fixture = Fixture(client, client.service(), impl)
            withContext(Dispatchers.Default) { fixture.block() }
        }
    }

    private fun Fixture.method(name: String) =
        client.serviceDescriptor(BlobService::class.java).methods.single { it.method.name == name }

    private fun store() = NatsConnectionManager.connection().objectStore(PayloadBucket.TRANSIENT.bucketName)

    private fun liveObjects(): Set<String> {
        val management = NatsConnectionManager.connection().objectStoreManagement()
        if (PayloadBucket.TRANSIENT.bucketName !in management.bucketNames) return emptySet()
        return store().list.filterNot { it.isDeleted }.mapTo(HashSet()) { it.objectName }
    }

    companion object {
        private const val LARGE = 5 * 1024 * 1024

        fun blob(size: Int) = ByteArray(size) { (it * 31 + it / 7).toByte() }

        fun ByteArray.checksum(): Long = fold(17L) { acc, b -> acc * 31 + b }
    }
}
