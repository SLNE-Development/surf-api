package dev.slne.surf.nats.event

import dev.slne.surf.nats.NatsEvent
import dev.slne.surf.nats.WireFormat
import dev.slne.surf.nats.codec.NatsCodec
import kotlinx.serialization.Contextual
import kotlinx.serialization.modules.EmptySerializersModule
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventDescriptorTest {

    @NatsEvent
    data class PlayerHealthChanged(val player: @Contextual UUID, val health: Double)

    @NatsEvent("created", format = WireFormat.JSON)
    data class PunishmentCreated(val id: Long)

    @NatsEvent("panel.punishment.created", prefixed = false)
    data class ExternalEvent(val id: Long)

    @NatsEvent("invalid subject")
    data class InvalidSubjectEvent(val id: Long)

    data class NotAnEvent(val id: Long)

    private val codec = NatsCodec(EmptySerializersModule())

    private fun describe(type: Class<*>) = EventDescriptor.of(type, "punishment", codec)

    @Test
    fun `default subject is the kebab-case class name under the client prefix`() {
        val descriptor = describe(PlayerHealthChanged::class.java)

        assertEquals("punishment.player-health-changed", descriptor.subject)
        assertEquals(WireFormat.CBOR, descriptor.format)
    }

    @Test
    fun `explicit subject is relative to the client prefix`() {
        val descriptor = describe(PunishmentCreated::class.java)

        assertEquals("punishment.created", descriptor.subject)
        assertEquals(WireFormat.JSON, descriptor.format)
    }

    @Test
    fun `unprefixed subject is used as is`() {
        assertEquals("panel.punishment.created", describe(ExternalEvent::class.java).subject)
    }

    @Test
    fun `invalid explicit subject is rejected`() {
        assertFailsWith<IllegalArgumentException> { describe(InvalidSubjectEvent::class.java) }
    }

    @Test
    fun `class without NatsEvent is rejected`() {
        assertFailsWith<IllegalArgumentException> { describe(NotAnEvent::class.java) }
    }

    @Test
    fun `serializer generated through MetaSerializable roundtrips in both formats`() {
        val descriptor = describe(PlayerHealthChanged::class.java)
        val event = PlayerHealthChanged(UUID.randomUUID(), 17.5)

        for (format in WireFormat.entries) {
            val bytes = codec.encode(descriptor.serializer, event, format)
            assertEquals(event, codec.decode(descriptor.serializer, bytes, format.contentType), "format $format")
        }
    }
}
