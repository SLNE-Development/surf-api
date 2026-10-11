package dev.slne.surf.nats.client

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.event.NatsEvent
import dev.slne.surf.nats.event.NatsEventContext
import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.NatsFormat
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.event.natsEventContext
import dev.slne.surf.nats.event.natsEventContextOrNull
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
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
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NatsClientListenerTest {

    @NatsEvent("created")
    data class Created(val id: Long)

    @NatsEvent("json")
    @NatsFormat(WireFormat.JSON)
    data class JsonEvent(val playerName: String)

    @NatsEvent("panel.listener-test.created", prefixed = false)
    data class PanelEvent(val id: Long)

    @NatsEvent("balanced")
    data class Balanced(val id: Int)

    @NatsEvent("shared")
    data class SharedSubjectA(val id: Long)

    @NatsEvent("shared")
    data class SharedSubjectB(val id: Long)

    data class NotAnEvent(val id: Long)

    @NatsEvent("listener-test.origin", prefixed = false)
    data class OriginEvent(val id: Long)

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

    class JsonListener {
        val recorder = Recorder<JsonEvent>()

        @NatsListener
        fun on(event: JsonEvent) {
            recorder.received.trySend(event)
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

        @NatsListener(loadBalanced = true)
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

    class NoListenerMethods {
        fun on(event: Created) = Unit
    }

    class ConflictingListener {
        val recorder = Recorder<SharedSubjectA>()

        @NatsListener
        fun onA(event: SharedSubjectA) {
            recorder.received.trySend(event)
        }

        @NatsListener
        fun onB(event: SharedSubjectB) = Unit
    }

    class SharedSubjectAListener {
        val recorder = Recorder<SharedSubjectA>()

        @NatsListener
        fun on(event: SharedSubjectA) {
            recorder.received.trySend(event)
        }
    }

    class IgnoreOwnListener {
        val recorder = Recorder<OriginEvent>()

        @NatsListener(ignoreOwn = true)
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

        @NatsListener(ignoreOwn = true)
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
        @NatsListener(loadBalanced = true, ignoreOwn = true)
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
    fun `json event roundtrips through publish and listener`() = withClient("listen-json") { client ->
        val listener = JsonListener()
        client.registerListener(listener)

        client.publish(JsonEvent("Twisti"))

        assertEquals(JsonEvent("Twisti"), listener.recorder.receiveWithin())
    }

    @Test
    fun `json published by an external party is received`() = withClient("listen-panel") { client ->
        val listener = JsonListener()
        client.registerListener(listener)

        val headers = Headers().add(NatsHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
        NatsConnectionManager.connection()
            .publish("listen-panel.json", headers, """{"player_name":"Panel"}""".encodeToByteArray())

        assertEquals(JsonEvent("Panel"), listener.recorder.receiveWithin())
    }

    @Test
    fun `unprefixed event is received on the plain subject`() = withClient("listen-unprefixed") { client ->
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
            val otherMember = connection.subscribe("listen-balanced.balanced", "listen-balanced")
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
                            val descriptor = client.eventDescriptor(Balanced::class.java)
                            val contentType = message.headers?.getFirst(NatsHeaders.CONTENT_TYPE)
                            delivered += (client.codec.decode(descriptor.serializer, message.data, contentType) as Balanced).id
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
        assertContains(message, "notAnEvent")
    }

    @Test
    fun `object without listener methods is rejected`() = withClient("listen-none") { client ->
        assertFailsWith<IllegalArgumentException> { client.registerListener(NoListenerMethods()) }
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
        client.close()

        val descriptor = (NatsClient.create("listen-close-probe") as NatsClientImpl).use { probe ->
            probe.eventDescriptor(Created::class.java)
        }
        val bytes = (client as NatsClientImpl).codec.encode(descriptor.serializer, Created(1), WireFormat.CBOR)
        val headers = Headers().add(NatsHeaders.CONTENT_TYPE, WireFormat.CBOR.contentType)
        NatsConnectionManager.connection().publish("listen-close-client.created", headers, bytes)

        listener.recorder.assertNothingWithin()
    }

    @Test
    fun `undecodable payload is dropped and later events still arrive`() = withClient("listen-garbage") { client ->
        val listener = CreatedListener()
        client.registerListener(listener)

        val headers = Headers().add(NatsHeaders.CONTENT_TYPE, WireFormat.CBOR.contentType)
        NatsConnectionManager.connection().publish("listen-garbage.created", headers, byteArrayOf(0x7f, 0x01, 0x02))
        client.publish(Created(9))

        assertEquals(Created(9), listener.recorder.receiveWithin())
    }

    @Test
    fun `failed registration leaves no listener subscribed`() = withClient("listen-conflict") { client ->
        val conflicting = ConflictingListener()
        assertFailsWith<IllegalStateException> { client.registerListener(conflicting) }

        val probe = SharedSubjectAListener()
        client.registerListener(probe)
        client.publish(SharedSubjectA(3))

        assertEquals(SharedSubjectA(3), probe.recorder.receiveWithin())
        conflicting.recorder.assertNothingWithin()
    }

    @Test
    fun `ignoreOwn skips events of the same client and keeps events of other clients`() =
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
    fun `listener without ignoreOwn receives own events`() = withClient("listen-own-default") { client ->
        val listener = AllOriginsListener()
        client.registerListener(listener)

        client.publish(OriginEvent(3))

        assertEquals(OriginEvent(3), listener.recorder.receiveWithin())
    }

    @Test
    fun `ignoreOwn is applied per listener method`() = withClient("listen-own-mixed-a") { client ->
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
            assertTrue(ownContext.isOwn)
            assertEquals(client.origin, ownContext.origin)

            other.publish(OriginEvent(7))
            val (foreignEvent, foreignContext) = listener.recorder.receiveWithin()
            assertEquals(OriginEvent(7), foreignEvent)
            assertFalse(foreignContext.isOwn)
            assertEquals(other.origin, foreignContext.origin)
        }
    }

    @Test
    fun `event without origin header is foreign`() = withClient("listen-no-origin") { client ->
        val context = ContextListener()
        val ignoring = IgnoreOwnListener()
        client.registerListener(context)
        client.registerListener(ignoring)

        val descriptor = client.eventDescriptor(OriginEvent::class.java)
        val bytes = client.codec.encode(descriptor.serializer, OriginEvent(8), WireFormat.CBOR)
        val headers = Headers().add(NatsHeaders.CONTENT_TYPE, WireFormat.CBOR.contentType)
        NatsConnectionManager.connection().publish("listener-test.origin", headers, bytes)

        val (event, eventContext) = context.recorder.receiveWithin()
        assertEquals(OriginEvent(8), event)
        assertNull(eventContext.origin)
        assertFalse(eventContext.isOwn)
        assertEquals(OriginEvent(8), ignoring.recorder.receiveWithin())
    }

    @Test
    fun `ignoreOwn cannot be combined with loadBalanced`() = withClient("listen-own-balanced") { client ->
        val exception = assertFailsWith<IllegalArgumentException> {
            client.registerListener(BalancedIgnoreOwnListener())
        }
        assertContains(exception.message.orEmpty(), "balancedIgnoringOwn")
        assertContains(exception.message.orEmpty(), "ignoreOwn")
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

    private suspend fun Recorder<*>.assertNothingWithin(window: kotlin.time.Duration = 300.milliseconds) {
        withContext(Dispatchers.Default) { delay(window) }
        val unexpected = received.tryReceive().getOrNull()
        assertNull(unexpected, "unexpected event $unexpected")
    }
}
