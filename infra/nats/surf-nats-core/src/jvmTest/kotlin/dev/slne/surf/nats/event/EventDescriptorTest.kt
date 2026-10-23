package dev.slne.surf.nats.event

import dev.slne.surf.nats.InvalidSubjectEvent
import dev.slne.surf.nats.ReservedTokenEvent
import dev.slne.surf.nats.SingleTokenGlobal
import dev.slne.surf.nats.test.proto.v1.Created
import dev.slne.surf.nats.test.proto.v1.NotAnEvent
import dev.slne.surf.nats.test.proto.v1.OriginEvent
import dev.slne.surf.nats.test.proto.v1.PanelEvent
import dev.slne.surf.nats.test.proto.v1.ProfileUpdated
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import dev.slne.surf.nats.test.proto.v1.VersionedChanged as VersionedChangedV1
import dev.slne.surf.nats.test.proto.v2.VersionedChanged as VersionedChangedV2

class EventDescriptorTest {

    private fun describe(type: Class<*>) = EventDescriptor.of(type, "punishment")

    @Test
    fun `prefixed subject puts the major version after the client name`() {
        assertEquals("punishment.v1.created", describe(Created::class.java).subject)
        assertEquals("punishment.v1.profile-updated", describe(ProfileUpdated::class.java).subject)
        assertEquals("punishment.v1.versioned.changed", describe(VersionedChangedV1::class.java).subject)
        assertEquals("punishment.v2.versioned.changed", describe(VersionedChangedV2::class.java).subject)
    }

    @Test
    fun `global subject puts the major version after its first token`() {
        assertEquals("panel.v1.listener-test.created", describe(PanelEvent::class.java).subject)
        assertEquals("listener-test.v1.origin", describe(OriginEvent::class.java).subject)
        assertEquals("roles.v1", describe(SingleTokenGlobal::class.java).subject)
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
    fun `invalid subjects of hand-built descriptors are rejected`() {
        val invalid = assertFailsWith<IllegalArgumentException> { describe(InvalidSubjectEvent::class.java) }
        assertContains(invalid.message.orEmpty(), "punishment.v1.invalid subject")

        val reserved = assertFailsWith<IllegalArgumentException> { describe(ReservedTokenEvent::class.java) }
        assertContains(reserved.message.orEmpty(), "tokens starting with '$' are reserved")
    }
}
