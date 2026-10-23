package dev.slne.surf.nats.interop

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.TEST_UUID
import dev.slne.surf.nats.client.ClientOrigin
import dev.slne.surf.nats.client.NatsClientImpl
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.eventually
import dev.slne.surf.nats.messageCount
import dev.slne.surf.nats.protocol.PayloadBucket
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.registerService
import dev.slne.surf.nats.rpc.Answer
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.test.proto.v1.Blob
import dev.slne.surf.nats.test.proto.v1.BlobService
import dev.slne.surf.nats.test.proto.v1.CalcService
import dev.slne.surf.nats.test.proto.v1.Color
import dev.slne.surf.nats.test.proto.v1.ColorHolder
import dev.slne.surf.nats.test.proto.v1.Count
import dev.slne.surf.nats.test.proto.v1.FindReply
import dev.slne.surf.nats.test.proto.v1.FinderService
import dev.slne.surf.nats.test.proto.v1.HealthReply
import dev.slne.surf.nats.test.proto.v1.InteropEvent
import dev.slne.surf.nats.test.proto.v1.InteropEvolved
import dev.slne.surf.nats.test.proto.v1.InteropEvolvedV2
import dev.slne.surf.nats.test.proto.v1.InteropScalars
import dev.slne.surf.nats.test.proto.v1.InteropService
import dev.slne.surf.nats.test.proto.v1.InteropTypes
import dev.slne.surf.nats.test.proto.v1.InteropVectorService
import dev.slne.surf.nats.test.proto.v1.LookupReply
import dev.slne.surf.nats.test.proto.v1.Number
import dev.slne.surf.nats.test.proto.v1.OrderService
import dev.slne.surf.nats.test.proto.v1.OriginEvent
import dev.slne.surf.nats.test.proto.v1.OwnerReply
import dev.slne.surf.nats.test.proto.v1.PanelEvent
import dev.slne.surf.nats.test.proto.v1.RolesService
import dev.slne.surf.nats.test.proto.v1.Text
import dev.slne.surf.nats.test.proto.v1.UnsignedReply
import dev.slne.surf.nats.test.proto.v1.WideColorHolder
import dev.slne.surf.nats.test.fixtures.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag as JUnitTag
import org.junit.jupiter.api.TestInstance
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import dev.slne.surf.nats.test.proto.v1.VersionedPingReply as V1PingReply
import dev.slne.surf.nats.test.proto.v1.VersionedService as V1VersionedService
import dev.slne.surf.nats.test.proto.v2.VersionedPingReply as V2PingReply
import dev.slne.surf.nats.test.proto.v2.VersionedService as V2VersionedService

/**
 * Runs `src/jsTest/interop/interop.mjs` (surf-nats-js on Node, through the npm distribution and its TypeScript
 * facade) against the JVM runtime: one script scenario per test, plus the wire vectors of [InteropVectors].
 *
 * Two clients named [OWNER] stand in for two instances of one JVM service. Only `jvmInteropTest` runs this class.
 */
@JUnitTag("js-interop")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JsInteropTest {

    data class Received(val instance: String, val kind: String, val event: Any)

    class InteropListener(private val instance: String, private val received: ConcurrentLinkedQueue<Received>) {
        @NatsListener
        fun onAll(event: InteropEvent) {
            received += Received(instance, "all", event)
        }

        @NatsListener(recipients = Recipients.ANY_INSTANCE)
        fun onAny(event: InteropEvent) {
            received += Received(instance, "any", event)
        }

        @NatsListener
        fun onGlobal(event: OriginEvent) {
            received += Received(instance, "global", event)
        }
    }

    class CalcImpl : CalcService {
        val fires = AtomicInteger()

        override suspend fun mixed(a: Int, b: Long, factor: Double, flag: Boolean, label: String) =
            Text("$a|$b|$factor|$flag|$label")

        override suspend fun fire() {
            fires.incrementAndGet()
        }

        override suspend fun fail(message: String): Text = throw IllegalStateException(message)

        override suspend fun slow(): Text {
            delay(3.seconds)
            return Text("late")
        }

        override suspend fun health(): HealthReply = unused()
        override suspend fun innerTimeout(): Text = unused()
        override suspend fun count(): Count = unused()
        override suspend fun slowFire() = unused()
        override suspend fun gated(): Text = unused()
        override suspend fun unsigned(id: Long, small: Int, optionalId: Long?): UnsignedReply = unused()
        override suspend fun inline(duration: Duration, maybeDuration: Duration?, tag: Tag, maybeTag: Tag?): Text =
            unused()

        override suspend fun missing(): Text = unused()
    }

    class FinderImpl(val instance: String) : FinderService {
        val broadcasts = ConcurrentLinkedQueue<String>()

        override suspend fun find(key: String): Answer<FindReply> {
            return if (key == instance) Answer.Known(FindReply("from-$instance")) else Answer.Unknown
        }

        override suspend fun broadcast(message: String) {
            broadcasts += message
        }

        override suspend fun lookup(key: String): FindReply = unused()
    }

    class OrderImpl : OrderService {
        val placed = ConcurrentLinkedQueue<Long>()
        val bigSizes = ConcurrentLinkedQueue<Int>()

        override suspend fun place(id: Long) {
            placed += id
        }

        override suspend fun total(id: Long): Number = Number(id * 2)

        override suspend fun lookup(id: Long): LookupReply = LookupReply(null)

        override suspend fun big(data: ByteArray) {
            bigSizes += if (data.contentEquals(blob(data.size))) data.size else -1
        }

        override suspend fun echo(data: ByteArray): Blob = Blob(data)

        override suspend fun audit(id: Long): Number = unused()
        override suspend fun paint(
            color: Color,
            maybeColor: Color?,
            colors: List<Color>,
            colorsByName: Map<String, Color>,
        ) = unused()

        override suspend fun ping(): Text = unused()
    }

    class BlobImpl : BlobService {
        // The size when the blob has the blob(size) content, -1 otherwise
        override suspend fun checksum(data: ByteArray): Number {
            return Number(if (data.contentEquals(blob(data.size))) data.size.toLong() else -1L)
        }

        override suspend fun create(size: Int): Blob = Blob(blob(size))

        override suspend fun echo(data: ByteArray): Blob = Blob(data)
    }

    class RolesImpl : RolesService {
        val whitelisted = ConcurrentLinkedQueue<Pair<String, Long>>()

        override suspend fun whitelist(action: String, userId: Long) {
            whitelisted += action to userId
        }

        override suspend fun echo(id: Long): Number = Number(id)

        override suspend fun owner(player: UUID): OwnerReply = unused()
        override suspend fun store(id: Long, payload: String?) = unused()
        override suspend fun handle(id: Long, payload: String?): Number = unused()
    }

    class VersionedV1 : V1VersionedService {
        override suspend fun ping(message: String) = V1PingReply(message, 1)
    }

    class VersionedV2 : V2VersionedService {
        override suspend fun ping(message: String, attempt: Int) = V2PingReply("$message#$attempt", 2, "eu")
    }

    private val received = ConcurrentLinkedQueue<Received>()
    private val book = VectorBook(InteropVectors.all)
    private val calc = CalcImpl()
    private val finders = listOf(FinderImpl(PRIMARY), FinderImpl(SECONDARY))
    private val orders = OrderImpl()
    private val roles = RolesImpl()

    private lateinit var primary: NatsClientImpl
    private lateinit var secondary: NatsClientImpl
    private lateinit var raw: RawVectorResponder
    private lateinit var vectorFile: Path

    @BeforeAll
    fun setUp(): Unit = runBlocking {
        NatsInstance.instance.load()
        primary = NatsClient.create(OWNER) as NatsClientImpl
        // A second instance of the same client; the factory allows one client per name and process
        secondary = NatsClientImpl(OWNER, ClientOrigin.capture()) {}

        for ((client, finder) in listOf(primary, secondary).zip(finders)) {
            client.registerListener(InteropListener(finder.instance, received))
            client.registerService<FinderService>(finder)
            client.registerService<OrderService>(orders)
        }
        primary.registerService<InteropVectorService>(VectorProvider(book))
        primary.registerService<InteropService>(VectorEchoes(book))
        primary.registerService<CalcService>(calc)
        primary.registerService<BlobService>(BlobImpl())
        primary.registerService<RolesService>(roles)
        primary.registerService<V1VersionedService>(VersionedV1())
        primary.registerService<V2VersionedService>(VersionedV2())
        raw = RawVectorResponder(RAW_OWNER, book)

        vectorFile = Files.createTempFile("surf-nats-interop-vectors", ".json")
        vectorFile.writeText(InteropVectors.all.joinToString(",", "{", "}") { "\"${it.name}\":\"${it.hex()}\"" })
    }

    @AfterAll
    fun tearDown(): Unit = runBlocking {
        raw.close()
        secondary.close()
        primary.close()
        NatsInstance.instance.disable()
        Files.deleteIfExists(vectorFile)
    }

    // ---- events ----

    @Test
    fun `JS events reach the ANY and ALL listeners of both instances`() = interop {
        received.clear()
        assertEquals(listOf("prefixed", "global"), run("publish-events").checks)

        eventually("events must reach every listener") { received.size >= 5 }
        delay(SETTLE) // late duplicates of the ANY_INSTANCE delivery

        assertEquals(setOf(PRIMARY, SECONDARY), received.filter { it.kind == "all" }.map { it.instance }.toSet())
        assertEquals(2, received.count { it.kind == "all" }, "one ALL_INSTANCES delivery per instance")
        assertEquals(1, received.count { it.kind == "any" }, "one ANY_INSTANCE delivery per client name")
        assertEquals(2, received.count { it.kind == "global" }, "global event on both instances")
        received.filter { it.kind != "global" }.forEach { assertEquals(EVENT, it.event) }
        received.filter { it.kind == "global" }.forEach { assertEquals(OriginEvent(7), it.event) }
    }

    @Test
    fun `JVM events reach the JS listeners`() = interop {
        NodeScript.start("listen-events", environment()).use { script ->
            script.awaitReady("listening")
            primary.publish(EVENT)
            primary.publish(PanelEvent(9))
            assertEquals(listOf("all", "any", "global"), script.awaitSuccess().checks)
        }
    }

    // ---- core calls ----

    @Test
    fun `JS calls ANY_INSTANCE methods with a value and with Unit`() = interop {
        val fires = calc.fires.get()
        assertEquals(listOf("value", "unit"), run("call-any").checks)
        assertEquals(fires + 1, calc.fires.get())
    }

    @Test
    fun `JVM failures, missing responders and timeouts become the JS errors`() = interop {
        assertEquals(listOf("remote-error", "no-responders", "timeout"), run("call-errors").checks)
    }

    @Test
    fun `JS callAnswer gets Known and Unknown from ALL_INSTANCES responders`() = interop {
        assertEquals(listOf("known", "unknown"), run("call-answer").checks)
    }

    @Test
    fun `JS ALL_INSTANCES fan-out reaches both instances`() = interop {
        finders.forEach { it.broadcasts.clear() }
        assertEquals(listOf("sent"), run("fan-out").checks)

        eventually("both instances must receive the fan-out") { finders.all { it.broadcasts.isNotEmpty() } }
        delay(SETTLE)
        finders.forEach { assertEquals(listOf("hello"), it.broadcasts.toList()) }
    }

    // ---- persistent calls ----

    @Test
    fun `JS persistent fire runs the JVM handler once`() = interop {
        orders.placed.clear()
        assertEquals(listOf("fired"), run("persistent-fire").checks)

        eventually("the durable consumer must run the handler") { orders.placed.isNotEmpty() }
        delay(SETTLE)
        assertEquals(listOf(42L), orders.placed.toList(), "one handler run across both instances")
        val stream = primary.serviceDescriptor(OrderService::class.java).streams!!.stream
        eventually("the work queue must be empty after the ack") { messageCount(stream) == 0L }
    }

    @Test
    fun `JS persistent RPC is answered by the JVM`() = interop {
        assertEquals(listOf("value", "nullable"), run("persistent-rpc").checks)
    }

    // ---- payload offloading ----

    @Test
    fun `large core payloads go through the object store in both directions`() = interop {
        assertEquals(listOf("request", "response", "both"), run("large-core").checks)
        eventually("every payload object must be deleted") { liveObjects(PayloadBucket.TRANSIENT).isEmpty() }
    }

    @Test
    fun `large persistent payloads go through the object store in both directions`() = interop {
        orders.bigSizes.clear()
        assertEquals(listOf("rpc", "fire"), run("large-persistent").checks)

        eventually("the persistent fire must run") { orders.bigSizes.isNotEmpty() }
        assertEquals(listOf(LARGE), orders.bigSizes.toList())
        eventually("every payload object must be deleted") {
            liveObjects(PayloadBucket.TRANSIENT).isEmpty() && liveObjects(PayloadBucket.PERSISTENT).isEmpty()
        }
    }

    // ---- contract variants ----

    @Test
    fun `JS calls v1 and v2 of one service side by side`() = interop {
        assertEquals(listOf("v1", "v2"), run("versioned").checks)
    }

    @Test
    fun `JS calls a global service without an owner`() = interop {
        roles.whitelisted.clear()
        assertEquals(listOf("rpc", "fire"), run("global-service").checks)
        eventually("the global persistent fire must run") { roles.whitelisted.isNotEmpty() }
        assertEquals(listOf("ADD" to 42L), roles.whitelisted.toList())
    }

    // ---- wire vectors ----

    @Test
    fun `Kotlin encodings of the wire vectors`() {
        val hex = InteropVectors.all.associate { it.name to it.hex() }
        for (name in EMPTY_VECTORS) assertEquals("", hex[name], "$name must encode to 0 bytes")
        // kotlinx compares the ByteArray default by reference, so `data` [default = "\001\002"] is never elided
        assertEquals("32020102", hex["defaults-elided"])
        assertEquals("32020102", hex["defaults-sent-by-js"])
        // uint32 values >= 2^31 are written as sign-extended 10-byte varints (int32 encoding of a negative Int)
        assertTrue(hex.getValue("scalars-extremes").startsWith("08ffffffffffffffffff01"), "uint32 0xffffffff")
        assertTrue(hex.getValue("types").startsWith("0a120a10123e4567e89b12d3a456426614174000"), "big-endian Uuid")
        assertEquals("0a120a10800000000000000000000000000000011200" + "1a00", hex["types-epoch"])
        assertEquals("0a00", hex["oneof-text"])
        assertEquals("1a00", hex["oneof-empty"])

        // Kotlin's own reading of the unknown enum values matches what the script must send back
        val wide = NatsCodec.encode(WideColorHolder.serializer(), InteropVectors.unknownColorsWide)
        val decoded = NatsCodec.decode(ColorHolder.serializer(), wide, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
        assertEquals(InteropVectors.all.single { it.name == "colors-unknown-from-kotlin" }.echo, decoded)

        // The older Kotlin reader skips the newer fields; the newer one restores null and the [default]
        val newer = InteropVectors.all.single { it.name == "evolved-new-to-old" }
        assertEquals(newer.echo, NatsCodec.decode(InteropEvolved.serializer(), newer.encode(), ProtocolHeaders.PROTOBUF_CONTENT_TYPE))
        val older = InteropVectors.all.single { it.name == "evolved-old-to-new" }
        assertEquals(older.echo, NatsCodec.decode(InteropEvolvedV2.serializer(), older.encode(), ProtocolHeaders.PROTOBUF_CONTENT_TYPE))
    }

    @Test
    fun `wire vectors round-trip between Kotlin and protobuf-es`() = interop {
        val result = run("vectors")
        assertEquals(emptyList(), book.mismatches.toList())
        assertEquals(InteropVectors.all.map { it.name }.sorted(), result.vectors.sorted())
        for (vector in InteropVectors.all) {
            val expected = if (vector.echo == null) 0 else 2
            assertEquals(expected, book.echoCount(vector.name), "echoes of ${vector.name}")
        }
    }

    // ---- helpers ----

    private fun environment() = mapOf(
        "SURF_NATS_INTEROP_OWNER" to OWNER,
        "SURF_NATS_INTEROP_RAW_OWNER" to RAW_OWNER,
        "SURF_NATS_INTEROP_VECTORS" to vectorFile.toString(),
    )

    private suspend fun run(scenario: String): NodeScript.NodeResult {
        return NodeScript.start(scenario, environment()).use { it.awaitSuccess() }
    }

    private fun liveObjects(bucket: PayloadBucket): Set<String> {
        val connection = NatsConnectionManager.connection()
        if (bucket.bucketName !in connection.objectStoreManagement().bucketNames) return emptySet()
        return connection.objectStore(bucket.bucketName).list.filterNot { it.isDeleted }.mapTo(HashSet()) { it.objectName }
    }

    private fun interop(block: suspend CoroutineScope.() -> Unit) = runTest(timeout = 3.minutes) {
        withContext(Dispatchers.Default) { block() }
    }

    companion object {
        const val OWNER = "interop-jvm"
        const val RAW_OWNER = "interop-raw"
        const val PRIMARY = "primary"
        const val SECONDARY = "secondary"
        const val LARGE = 2 * 1024 * 1024

        private val SETTLE = 500.milliseconds

        private val EMPTY_VECTORS = listOf(
            "maps-empty", "packed-empty", "nullables-null", "empty", "oneof-unset", "required-missing",
        )

        // Mirrors EVENT in src/jsTest/interop/scenarios.mjs
        val EVENT = InteropEvent(
            scalars = InteropScalars(
                unsignedSmall = Int.MIN_VALUE,
                unsignedLarge = -1L,
                signedLarge = -5L,
                small = -7,
                large = Long.MAX_VALUE,
                fixedSmall = -1,
                ratio = 0.5,
                flag = true,
            ),
            types = InteropTypes(TEST_UUID, InteropVectors.INSTANT, InteropVectors.NEGATIVE_DURATION),
        )

        // Mirrors blob() in src/jsTest/interop/support.mjs
        fun blob(size: Int) = ByteArray(size) { (it * 31 + it / 7).toByte() }

        private fun unused(): Nothing = throw UnsupportedOperationException("not used by the interop script")
    }
}
