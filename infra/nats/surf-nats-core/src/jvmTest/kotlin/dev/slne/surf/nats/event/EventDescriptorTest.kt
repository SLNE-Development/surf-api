package dev.slne.surf.nats.event

import dev.slne.surf.nats.InvalidProtoNameEvent
import dev.slne.surf.nats.test.proto.v1.Created
import dev.slne.surf.nats.test.proto.v1.NotAnEvent
import dev.slne.surf.nats.test.proto.v1.ProfileUpdated
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import dev.slne.surf.nats.test.proto.v1.VersionedChanged as VersionedChangedV1
import dev.slne.surf.nats.test.proto.v2.VersionedChanged as VersionedChangedV2

class EventDescriptorTest {

    private fun describe(type: Class<*>) = EventDescriptor.of(type)

    @Test
    fun `subject is the proto full name under surf evt`() {
        assertEquals("surf.evt.surf.nats.test.v1.Created", describe(Created::class.java).subject)
        assertEquals("surf.evt.surf.nats.test.v1.ProfileUpdated", describe(ProfileUpdated::class.java).subject)
    }

    @Test
    fun `major versions of an event have separate subjects`() {
        assertEquals("surf.evt.surf.nats.test.v1.VersionedChanged", describe(VersionedChangedV1::class.java).subject)
        assertEquals("surf.evt.surf.nats.test.v2.VersionedChanged", describe(VersionedChangedV2::class.java).subject)
    }

    @Test
    fun `serializer comes from the generated descriptor`() {
        val descriptor = describe(Created::class.java)

        assertSame<Class<*>>(Created::class.java, descriptor.type)
        assertSame<Any>(Created.NATS_DESCRIPTOR.serializer, descriptor.serializer)
    }

    @Test
    fun `class without a generated descriptor is rejected`() {
        val message = assertFailsWith<IllegalArgumentException> { describe(NotAnEvent::class.java) }.message.orEmpty()

        assertContains(message, "${NotAnEvent::class.java.name} is not a generated surf-nats event")
    }

    @Test
    fun `invalid proto name of a hand-built descriptor is rejected`() {
        val message = assertFailsWith<IllegalArgumentException> {
            describe(InvalidProtoNameEvent::class.java)
        }.message.orEmpty()

        assertContains(message, "Invalid NATS_DESCRIPTOR of ${InvalidProtoNameEvent::class.java.name}")
        assertContains(message, "event surf.nats.test.v1.Invalid Name")
    }
}
