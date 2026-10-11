package dev.slne.surf.nats.client

import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.event.NatsEvent
import dev.slne.surf.nats.NatsInstance
import dev.slne.surf.nats.NatsFormat
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.codec.NatsHeaders
import dev.slne.surf.nats.connection.NatsConnectionManager
import io.nats.client.Message
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.time.Duration
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NatsClientPublishTest {

    @NatsEvent("created")
    data class PunishmentCreated(val id: Long, val reason: String)

    @NatsEvent("json-created")
    @NatsFormat(WireFormat.JSON)
    data class JsonPunishmentCreated(val punishmentId: Long)

    @NatsEvent("panel.punishment.created", prefixed = false)
    data class PanelPunishmentCreated(val id: Long)

    @NatsEvent
    data class BigEvent(val payload: String)

    data class NotAnEvent(val id: Long)

    @BeforeAll
    fun connect() = runBlocking { NatsInstance.instance.load() }

    @AfterAll
    fun disconnect() = runBlocking { NatsInstance.instance.disable() }

    @Test
    fun `cbor event is published with headers and decodable body`() = withClient("publish-cbor") { client ->
        val event = PunishmentCreated(42, "spam")

        val message = publishAndReceive(client, "publish-cbor.created", event)

        assertEquals(WireFormat.CBOR.contentType, message.headers.getFirst(NatsHeaders.CONTENT_TYPE))
        assertEquals("publish-cbor@${NatsConnectionManager.instanceId}", message.headers.getFirst(NatsHeaders.ORIGIN))
        assertNotNull(message.headers.getFirst(NatsHeaders.SENDER_VERSION))

        val descriptor = client.eventDescriptor(PunishmentCreated::class.java)
        val decoded = client.codec.decode(descriptor.serializers, message.data, WireFormat.CBOR.contentType)
        assertEquals(event, decoded)
    }

    @Test
    fun `json event is published as readable snake case json`() = withClient("publish-json") { client ->
        val message = publishAndReceive(client, "publish-json.json-created", JsonPunishmentCreated(42))

        assertEquals(WireFormat.JSON.contentType, message.headers.getFirst(NatsHeaders.CONTENT_TYPE))
        assertEquals("""{"punishment_id":42}""", message.data.decodeToString())
    }

    @Test
    fun `unprefixed event is published without the client prefix`() = withClient("publish-unprefixed") { client ->
        val message = publishAndReceive(client, "panel.punishment.created", PanelPunishmentCreated(7))
        assertEquals("panel.punishment.created", message.subject)
    }

    @Test
    fun `class without NatsEvent is rejected`() = withClient("publish-invalid") { client ->
        assertFailsWith<IllegalArgumentException> { client.publish(NotAnEvent(1)) }
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

    private fun publishAndReceive(client: NatsClient, subject: String, event: Any): Message {
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
