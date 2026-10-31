package dev.slne.surf.nats.client

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsConstants
import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.codec.NatsCodec
import dev.slne.surf.nats.connection.NatsConnectionManager
import dev.slne.surf.nats.descriptor.NatsEvent
import dev.slne.surf.nats.descriptor.NatsEventDescriptor
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.test.proto.v1.BigEvent
import dev.slne.surf.nats.test.proto.v1.Created
import dev.slne.surf.nats.test.proto.v1.Heartbeat
import dev.slne.surf.nats.test.proto.v1.PanelEvent
import io.nats.client.Message
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import kotlin.test.*
import dev.slne.surf.nats.test.proto.v1.VersionedChanged as VersionedChangedV1
import dev.slne.surf.nats.test.proto.v2.VersionedChanged as VersionedChangedV2

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NatsClientPublishTest {

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @Test
    fun `event is published as protobuf with headers on the subject of its proto name`() = withClient("publish-proto") { client ->
        val event = Created(42)

        val message = publishAndReceive(client, "surf.evt.surf.nats.test.v1.Created", event)

        val contentType = message.headers.getFirst(ProtocolHeaders.CONTENT_TYPE)
        assertEquals("application/protobuf", contentType)
        assertEquals("publish-proto@${NatsConnectionManager.instanceId}", message.headers.getFirst(ProtocolHeaders.ORIGIN))
        assertEquals(NatsConstants.VERSION, message.headers.getFirst(ProtocolHeaders.SENDER_VERSION))

        assertContentEquals(byteArrayOf(0x08, 0x2a), message.data)
        assertEquals(event, NatsCodec.decode(Created.serializer(), message.data, contentType))
    }

    @Test
    fun `differently named clients publish an event on the same subject`() = withClient("publish-same-a") { client ->
        withClient("publish-same-b") { other ->
            val subject = "surf.evt.surf.nats.test.v1.PanelEvent"
            assertEquals(subject, publishAndReceive(client, subject, PanelEvent(7)).subject)
            assertEquals(subject, publishAndReceive(other, subject, PanelEvent(8)).subject)
        }
    }

    @Test
    fun `major versions of an event are published on separate subjects`() = withClient("publish-versions") { client ->
        val v1 = publishAndReceive(client, "surf.evt.surf.nats.test.v1.VersionedChanged", VersionedChangedV1("one"))
        val v2 = publishAndReceive(client, "surf.evt.surf.nats.test.v2.VersionedChanged", VersionedChangedV2("two", 2))

        val contentType = ProtocolHeaders.PROTOBUF_CONTENT_TYPE
        assertEquals(VersionedChangedV1("one"), NatsCodec.decode(VersionedChangedV1.serializer(), v1.data, contentType))
        assertEquals(VersionedChangedV2("two", 2), NatsCodec.decode(VersionedChangedV2.serializer(), v2.data, contentType))
    }

    @Test
    fun `fieldless event is published with an empty body`() = withClient("publish-empty") { client ->
        val message = publishAndReceive(client, "surf.evt.surf.nats.test.v1.Heartbeat", Heartbeat())

        assertEquals(0, message.data?.size ?: 0)
    }

    @Test
    fun `event class without a generated descriptor is rejected`() = withClient("publish-invalid") { client ->
        val event = object : NatsEvent {
            override val natsDescriptor: NatsEventDescriptor<*> = Created.NATS_DESCRIPTOR
        }

        val exception = assertFailsWith<IllegalArgumentException> { client.publish(event) }
        assertContains(exception.message.orEmpty(), "is not a generated surf-nats event")
    }

    @Test
    fun `event above max payload is rejected with its type in the message`() = withClient("publish-big") { client ->
        val maxPayload = NatsConnectionManager.connection().serverInfo.maxPayload
        val event = BigEvent("x".repeat(maxPayload.toInt() + 1))

        val exception = assertFailsWith<IllegalArgumentException> { client.publish(event) }
        assertContains(exception.message.orEmpty(), BigEvent::class.java.name)
    }

    private fun withClient(name: String, block: (NatsClientImpl) -> Unit) {
        (NatsClient.create(name) as NatsClientImpl).use(block)
    }

    private fun publishAndReceive(client: NatsClient, subject: String, event: NatsEvent): Message {
        val connection = NatsConnectionManager.connection()
        val subscription = connection.subscribe(subject)
        try {
            client.publish(event)
            return assertNotNull(subscription.nextMessage(Duration.ofSeconds(5)), "no message on $subject")
        } finally {
            subscription.unsubscribe()
        }
    }
}
