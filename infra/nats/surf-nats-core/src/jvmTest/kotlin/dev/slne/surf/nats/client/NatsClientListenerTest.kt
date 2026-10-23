package dev.slne.surf.nats.client

import dev.slne.surf.nats.CreatedImpostor
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.TEST_UUID
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.event.NatsEventContext
import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.event.natsEventContext
import dev.slne.surf.nats.event.natsEventContextOrNull
import dev.slne.surf.nats.everything
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.rpc.Recipients
import dev.slne.surf.nats.test.proto.v1.Balanced
import dev.slne.surf.nats.test.proto.v1.Color
import dev.slne.surf.nats.test.proto.v1.Created
import dev.slne.surf.nats.test.proto.v1.Everything
import dev.slne.surf.nats.test.proto.v1.Heartbeat
import dev.slne.surf.nats.test.proto.v1.NotAnEvent
import dev.slne.surf.nats.test.proto.v1.OriginEvent
import dev.slne.surf.nats.test.proto.v1.PanelEvent
import dev.slne.surf.nats.test.proto.v1.PlayerEvent
import dev.slne.surf.nats.test.proto.v1.ProfileUpdated
import io.nats.client.JetStreamStatusException
import io.nats.client.impl.Headers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import java.util.*
import java.util.concurrent.ExecutionException
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import dev.slne.surf.nats.test.proto.v1.VersionedChanged as VersionedChangedV1
import dev.slne.surf.nats.test.proto.v2.VersionedChanged as VersionedChangedV2

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NatsClientListenerTest {

    class Recorder<T> {
        val received = Channel<T>(Channel.UNLIMITED)
    }

    class NormalAndSuspendListener {
        val normal = Recorder<Created>()
        val suspended = Recorder<Created>()

        @NatsListener
        fun onNormal(event: Created) {
            normal.received.trySend(event)
        }

        @NatsListener
        suspend fun onSuspend(event: Created) {
            yield()
            suspended.received.send(event)
        }
    }

    class CreatedListener {
        val recorder = Recorder<Created>()

        @NatsListener
        fun on(event: Created) {
            recorder.received.trySend(event)
        }
    }

    class EverythingListener {
        val recorder = Recorder<Everything>()

        @NatsListener
        fun on(event: Everything) {
            recorder.received.trySend(event)
        }
    }

    class PlayerListener {
        val recorder = Recorder<PlayerEvent>()

        @NatsListener
        fun on(event: PlayerEvent) {
            recorder.received.trySend(event)
        }
    }

    class ProfileListener {
        val recorder = Recorder<ProfileUpdated>()

        @NatsListener
        fun on(event: ProfileUpdated) {
            recorder.received.trySend(event)
        }
    }

    class HeartbeatListener {
        val recorder = Recorder<Heartbeat>()

        @NatsListener
        fun on(event: Heartbeat) {
            recorder.received.trySend(event)
        }
    }

    class VersionedListener {
        val v1 = Recorder<VersionedChangedV1>()
        val v2 = Recorder<VersionedChangedV2>()

        @NatsListener
        fun onV1(event: VersionedChangedV1) {
            v1.received.trySend(event)
        }

        @NatsListener
        fun onV2(event: VersionedChangedV2) {
            v2.received.trySend(event)
        }
    }

    class PanelListener {
        val recorder = Recorder<PanelEvent>()

        @NatsListener
        fun on(event: PanelEvent) {
            recorder.received.trySend(event)
        }
    }

    class BalancedListener {
        val recorder = Recorder<Balanced>()

        @NatsListener(recipients = Recipients.ANY_INSTANCE)
        fun on(event: Balanced) {
            recorder.received.trySend(event)
        }
    }

    class FailingAndWorkingListener {
        val recorder = Recorder<Created>()

        @NatsListener
        fun fails(event: Created): Unit = throw IllegalStateException("listener failure ${event.id}")

        @NatsListener
        fun works(event: Created) {
            recorder.received.trySend(event)
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    class InvalidListener {
        @NatsListener
        fun twoParameters(event: Created, other: Created) = Unit

        @NatsListener
        fun returnsValue(event: Created): Int = 1

        @NatsListener
        fun notAnEvent(event: NotAnEvent) = Unit
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    class ReturnsValueSuspendListener {
        @NatsListener
        suspend fun returnsValueSuspend(event: Created): Int = 1
    }

    class ImpostorBalancedListener {
        @NatsListener(recipients = Recipients.ANY_INSTANCE)
        fun on(event: CreatedImpostor) = Unit
    }

    class NoListenerMethods {
        fun on(event: Created) = Unit
    }

    open class BaseListener {
        val recorder = Recorder<Created>()

        @NatsListener
        open fun on(event: Created) {
            recorder.received.trySend(event)
        }
    }

    class UnannotatedOverrideListener : BaseListener() {
        override fun on(event: Created) = Unit
    }

    open class AnnotatedOverrideListener : BaseListener() {
        @NatsListener
        override fun on(event: Created) = super.on(event)
    }

    class UnannotatedDeepOverrideListener : AnnotatedOverrideListener() {
        override fun on(event: Created) = Unit
    }

    class ConflictingListener {
        val recorder = Recorder<Created>()

        @NatsListener
        fun onCreated(event: Created) {
            recorder.received.trySend(event)
        }

        @NatsListener
        fun onImpostor(event: CreatedImpostor) = Unit
    }

    class IgnoreOwnListener {
        val recorder = Recorder<OriginEvent>()

        @NatsListener(ignoreFromThisClient = true)
        fun on(event: OriginEvent) {
            recorder.received.trySend(event)
        }
    }

    class AllOriginsListener {
        val recorder = Recorder<OriginEvent>()

        @NatsListener
        fun on(event: OriginEvent) {
            recorder.received.trySend(event)
        }
    }

    class MixedListener {
        val ignoring = Recorder<OriginEvent>()
        val all = Recorder<OriginEvent>()

        @NatsListener(ignoreFromThisClient = true)
        fun ignoring(event: OriginEvent) {
            ignoring.received.trySend(event)
        }

        @NatsListener
        fun all(event: OriginEvent) {
            all.received.trySend(event)
        }
    }

    class ContextListener {
        val recorder = Recorder<Pair<OriginEvent, NatsEventContext>>()

        @NatsListener
        suspend fun on(event: OriginEvent) {
            recorder.received.send(event to natsEventContext())
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    class BalancedIgnoreOwnListener {
        @NatsListener(recipients = Recipients.ANY_INSTANCE, ignoreFromThisClient = true)
        fun balancedIgnoringOwn(event: Created) = Unit
    }

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @Test
    fun `normal and suspend listener in one object both receive the event`() = withClient("listen-both") { client ->
        val listener = NormalAndSuspendListener()
        client.registerListener(listener)

        client.publish(Created(1))

        assertEquals(Created(1), listener.normal.receiveWithin())
        assertEquals(Created(1), listener.suspended.receiveWithin())
    }

    @Test
    fun `every field kind roundtrips through publish and listener`() = withClient("listen-everything") { client ->
        val listener = EverythingListener()
        client.registerListener(listener)

        client.publish(everything())

        assertEquals(everything(), listener.recorder.receiveWithin())
    }

    @Test
    fun `protobuf published by an external party is received`() = withClient("listen-external") { client ->
        val listener = CreatedListener()
        client.registerListener(listener)

        val headers = Headers().add(ProtocolHeaders.CONTENT_TYPE, "Application/Protobuf; charset=binary")
        NatsConnectionManager.connection().publish("listen-external.v1.created", headers, byteArrayOf(0x08, 0x07))

        assertEquals(Created(7), listener.recorder.receiveWithin())
    }

    @Test
    fun `uuid event roundtrips`() = withClient("listen-uuid") { client ->
        val listener = PlayerListener()
        client.registerListener(listener)

        val uuid = UUID.randomUUID()
        client.publish(PlayerEvent(uuid))

        assertEquals(PlayerEvent(uuid), listener.recorder.receiveWithin())
    }

    @Test
    fun `null and present nullable fields roundtrip`() = withClient("listen-nullable") { client ->
        val listener = ProfileListener()
        client.registerListener(listener)

        val withoutOptionals = ProfileUpdated(TEST_UUID)
        val withOptionals = ProfileUpdated(TEST_UUID, nickname = "", previousOwner = UUID.randomUUID())
        client.publish(withoutOptionals)
        assertEquals(withoutOptionals, listener.recorder.receiveWithin())
        client.publish(withOptionals)
        assertEquals(withOptionals, listener.recorder.receiveWithin())
    }

    @Test
    fun `fieldless event roundtrips`() = withClient("listen-heartbeat") { client ->
        val listener = HeartbeatListener()
        client.registerListener(listener)

        client.publish(Heartbeat())

        assertEquals(Heartbeat(), listener.recorder.receiveWithin())
    }

    @Test
    fun `unknown enum number in an event decodes to the zero entry`() = withClient("listen-enum") { client ->
        val listener = EverythingListener()
        client.registerListener(listener)

        // A newer sender's color 99: field 18 as varint (tag 0x90 0x01) after the regular fields, so it wins
        val bytes = NatsCodec.encode(Everything.serializer(), everything()) + byteArrayOf(0x90.toByte(), 0x01, 99)
        val headers = Headers().add(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
        NatsConnectionManager.connection().publish("listen-enum.v1.everything", headers, bytes)

        assertEquals(everything().copy(color = Color.UNSPECIFIED), listener.recorder.receiveWithin())
    }

    @Test
    fun `major versions of an event reach only their own listeners`() = withClient("listen-versions") { client ->
        val listener = VersionedListener()
        client.registerListener(listener)

        client.publish(VersionedChangedV2("two", 2))
        assertEquals(VersionedChangedV2("two", 2), listener.v2.receiveWithin())
        listener.v1.assertNothingWithin()

        client.publish(VersionedChangedV1("one"))
        assertEquals(VersionedChangedV1("one"), listener.v1.receiveWithin())
        listener.v2.assertNothingWithin()
    }

    @Test
    fun `global event is received on its versioned subject`() = withClient("listen-global") { client ->
        val listener = PanelListener()
        client.registerListener(listener)

        client.publish(PanelEvent(5))

        assertEquals(PanelEvent(5), listener.recorder.receiveWithin())
    }

    @Test
    fun `load balanced listener shares events with other members of the queue group`() =
        withClient("listen-balanced") { client ->
            val listener = BalancedListener()
            client.registerListener(listener)

            val connection = NatsConnectionManager.connection()
            val otherMember = connection.subscribe("listen-balanced.v1.balanced", "listen-balanced")
            connection.flush(Duration.ofSeconds(5))

            val count = 50
            repeat(count) { client.publish(Balanced(it)) }
            connection.flush(Duration.ofSeconds(5))

            val delivered = mutableListOf<Int>()
            withContext(Dispatchers.Default) {
                withTimeout(10.seconds) {
                    while (delivered.size < count) {
                        listener.recorder.received.tryReceive().getOrNull()?.let { delivered += it.id }
                        otherMember.nextMessage(Duration.ofMillis(10))?.let { message ->
                            val contentType = message.headers?.getFirst(ProtocolHeaders.CONTENT_TYPE)
                            delivered += NatsCodec.decode(Balanced.serializer(), message.data, contentType).id
                        }
                    }
                }
            }
            otherMember.unsubscribe()

            assertEquals((0 until count).toList(), delivered.sorted(), "every event exactly once")
            listener.recorder.assertNothingWithin()
        }

    @Test
    fun `invalid listener methods are reported together`() = withClient("listen-invalid") { client ->
        val exception = assertFailsWith<IllegalArgumentException> { client.registerListener(InvalidListener()) }
        val message = exception.message.orEmpty()

        assertContains(message, "twoParameters")
        assertContains(message, "returnsValue")
        assertContains(message, "notAnEvent: parameter ${NotAnEvent::class.java.name} is not a generated surf-nats event")
    }

    @Test
    fun `suspend listener with a return value other than Unit is rejected`() =
        withClient("listen-suspend-value") { client ->
            val exception = assertFailsWith<IllegalArgumentException> {
                client.registerListener(ReturnsValueSuspendListener())
            }
            val message = exception.message.orEmpty()

            assertContains(message, "returnsValueSuspend")
            assertContains(message, "must return Unit")
        }

    @Test
    fun `same subject in a different queue group is rejected and keeps the first listener`() =
        withClient("listen-queue-conflict") { client ->
            val first = CreatedListener()
            client.registerListener(first)

            val exception = assertFailsWith<IllegalStateException> {
                client.registerListener(ImpostorBalancedListener())
            }
            assertContains(exception.message.orEmpty(), "is already used by")

            client.publish(Created(11))
            assertEquals(Created(11), first.recorder.receiveWithin())
        }

    @Test
    fun `object without listener methods is rejected`() = withClient("listen-none") { client ->
        assertFailsWith<IllegalArgumentException> { client.registerListener(NoListenerMethods()) }
    }

    @Test
    fun `override without @NatsListener is rejected`() = withClient("listen-override-unannotated") { client ->
        for (listener in listOf(UnannotatedOverrideListener(), UnannotatedDeepOverrideListener())) {
            val exception = assertFailsWith<IllegalArgumentException> { client.registerListener(listener) }
            val message = exception.message.orEmpty()

            assertContains(message, "on: overrides the @NatsListener method of")
            assertEquals(1, Regex("overrides").findAll(message).count(), "reported once: $message")
        }
    }

    @Test
    fun `annotated override is registered once`() = withClient("listen-override-annotated") { client ->
        val listener = AnnotatedOverrideListener()
        client.registerListener(listener)

        client.publish(Created(1))

        assertEquals(Created(1), listener.recorder.receiveWithin())
        listener.recorder.assertNothingWithin()
    }

    @Test
    fun `failing listener does not affect other listeners`() = withClient("listen-failing") { client ->
        val listener = FailingAndWorkingListener()
        client.registerListener(listener)

        client.publish(Created(1))
        client.publish(Created(2))

        val received = setOf(listener.recorder.receiveWithin(), listener.recorder.receiveWithin())
        assertEquals(setOf(Created(1), Created(2)), received)
    }

    @Test
    fun `closed registration no longer receives events`() = withClient("listen-close-reg") { client ->
        val closed = CreatedListener()
        val probe = CreatedListener()
        client.registerListener(closed).close()
        client.registerListener(probe)

        client.publish(Created(1))

        assertEquals(Created(1), probe.recorder.receiveWithin())
        closed.recorder.assertNothingWithin()
    }

    @Test
    fun `closed client no longer receives events`() = runTest {
        val client = NatsClient.create("listen-close-client")
        val listener = CreatedListener()
        client.registerListener(listener)
        client.publish(Created(1))
        assertEquals(Created(1), listener.recorder.receiveWithin())

        client.close()

        assertNoSubscriber("listen-close-client.v1.created")
    }

    @Test
    fun `undecodable payload is dropped and later events still arrive`() = withClient("listen-garbage") { client ->
        val listener = CreatedListener()
        client.registerListener(listener)

        val headers = Headers().add(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
        NatsConnectionManager.connection().publish("listen-garbage.v1.created", headers, byteArrayOf(0x7f, 0x01, 0x02))
        client.publish(Created(9))

        assertEquals(Created(9), listener.recorder.receiveWithin())
        listener.recorder.assertNothingWithin()
    }

    @Test
    fun `event without a non-null field is dropped and later events still arrive`() =
        withClient("listen-missing-field") { client ->
            val listener = CreatedListener()
            client.registerListener(listener)

            val headers = Headers().add(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
            NatsConnectionManager.connection().publish("listen-missing-field.v1.created", headers, ByteArray(0))
            client.publish(Created(10))

            assertEquals(Created(10), listener.recorder.receiveWithin())
            listener.recorder.assertNothingWithin()
        }

    @Test
    fun `event without the protobuf content type is dropped`() = withClient("listen-content-type") { client ->
        val listener = CreatedListener()
        client.registerListener(listener)

        val connection = NatsConnectionManager.connection()
        val body = NatsCodec.encode(Created.serializer(), Created(1))
        connection.publish("listen-content-type.v1.created", body)
        connection.publish(
            "listen-content-type.v1.created",
            Headers().add(ProtocolHeaders.CONTENT_TYPE, "application/cbor"),
            body
        )
        client.publish(Created(2))

        assertEquals(Created(2), listener.recorder.receiveWithin())
        listener.recorder.assertNothingWithin()
    }

    @Test
    fun `failed registration leaves no listener subscribed`() = withClient("listen-conflict") { client ->
        val conflicting = ConflictingListener()
        assertFailsWith<IllegalStateException> { client.registerListener(conflicting) }

        val probe = CreatedListener()
        client.registerListener(probe)
        client.publish(Created(3))

        assertEquals(Created(3), probe.recorder.receiveWithin())
        conflicting.recorder.assertNothingWithin()
    }

    @Test
    fun `ignoreFromThisClient skips events of the same client and keeps events of other clients`() =
        withClient("listen-own-a") { client ->
            val listener = IgnoreOwnListener()
            client.registerListener(listener)

            NatsClient.create("listen-own-b").use { other ->
                client.publish(OriginEvent(1))
                other.publish(OriginEvent(2))

                assertEquals(OriginEvent(2), listener.recorder.receiveWithin())
                listener.recorder.assertNothingWithin()
            }
        }

    @Test
    fun `listener without ignoreFromThisClient receives own events`() = withClient("listen-own-default") { client ->
        val listener = AllOriginsListener()
        client.registerListener(listener)

        client.publish(OriginEvent(3))

        assertEquals(OriginEvent(3), listener.recorder.receiveWithin())
    }

    @Test
    fun `ignoreFromThisClient is applied per listener method`() = withClient("listen-own-mixed-a") { client ->
        val listener = MixedListener()
        client.registerListener(listener)

        client.publish(OriginEvent(4))
        assertEquals(OriginEvent(4), listener.all.receiveWithin())
        listener.ignoring.assertNothingWithin()

        NatsClient.create("listen-own-mixed-b").use { other ->
            other.publish(OriginEvent(5))
            assertEquals(OriginEvent(5), listener.all.receiveWithin())
            assertEquals(OriginEvent(5), listener.ignoring.receiveWithin())
        }
    }

    @Test
    fun `event context reports origin and ownership`() = withClient("listen-ctx-a") { client ->
        val listener = ContextListener()
        client.registerListener(listener)

        (NatsClient.create("listen-ctx-b") as NatsClientImpl).use { other ->
            client.publish(OriginEvent(6))
            val (ownEvent, ownContext) = listener.recorder.receiveWithin()
            assertEquals(OriginEvent(6), ownEvent)
            assertTrue(ownContext.isFromThisClient)
            assertEquals(client.origin, ownContext.origin)

            other.publish(OriginEvent(7))
            val (foreignEvent, foreignContext) = listener.recorder.receiveWithin()
            assertEquals(OriginEvent(7), foreignEvent)
            assertFalse(foreignContext.isFromThisClient)
            assertEquals(other.origin, foreignContext.origin)
        }
    }

    @Test
    fun `event without origin header is foreign`() = withClient("listen-no-origin") { client ->
        val context = ContextListener()
        val ignoring = IgnoreOwnListener()
        client.registerListener(context)
        client.registerListener(ignoring)

        val bytes = NatsCodec.encode(OriginEvent.serializer(), OriginEvent(8))
        val headers = Headers().add(ProtocolHeaders.CONTENT_TYPE, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
        NatsConnectionManager.connection().publish("listener-test.v1.origin", headers, bytes)

        val (event, eventContext) = context.recorder.receiveWithin()
        assertEquals(OriginEvent(8), event)
        assertNull(eventContext.origin)
        assertFalse(eventContext.isFromThisClient)
        assertEquals(OriginEvent(8), ignoring.recorder.receiveWithin())
    }

    @Test
    fun `ignoreFromThisClient requires ALL_INSTANCES recipients`() = withClient("listen-own-balanced") { client ->
        val exception = assertFailsWith<IllegalArgumentException> {
            client.registerListener(BalancedIgnoreOwnListener())
        }
        assertContains(exception.message.orEmpty(), "balancedIgnoringOwn")
        assertContains(exception.message.orEmpty(), "ignoreFromThisClient")
    }

    @Test
    fun `event context is not available outside of listeners`() = runTest {
        assertNull(natsEventContextOrNull())
        assertFailsWith<IllegalStateException> { natsEventContext() }
    }

    private fun withClient(name: String, block: suspend (NatsClientImpl) -> Unit) = runTest {
        (NatsClient.create(name) as NatsClientImpl).use { block(it) }
    }

    private suspend fun <T> Recorder<T>.receiveWithin(timeout: kotlin.time.Duration = 5.seconds): T {
        return withContext(Dispatchers.Default) { withTimeout(timeout) { received.receive() } }
    }

    private suspend fun assertNoSubscriber(subject: String) = withContext(Dispatchers.IO) {
        val connection = NatsConnectionManager.connection()
        connection.flush(Duration.ofSeconds(5))
        val reply = connection.requestWithTimeout(subject, Headers(), ByteArray(0), Duration.ofSeconds(2))
        val failure = assertFailsWith<ExecutionException>("$subject still has a subscriber") { reply.get() }
        assertIs<JetStreamStatusException>(failure.cause)
    }

    private suspend fun Recorder<*>.assertNothingWithin(window: kotlin.time.Duration = 300.milliseconds) {
        withContext(Dispatchers.Default) { delay(window) }
        val unexpected = received.tryReceive().getOrNull()
        assertNull(unexpected, "unexpected event $unexpected")
    }
}
