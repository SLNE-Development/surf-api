package dev.slne.surf.nats.descriptor

import dev.slne.surf.nats.InheritedDescriptorEvent
import dev.slne.surf.nats.test.proto.v1.Created
import dev.slne.surf.nats.test.proto.v1.NotAnEvent
import dev.slne.surf.nats.test.proto.v1.NotifierService
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class GeneratedDescriptorsTest {

    abstract class NotifierImpl : NotifierService

    @Test
    fun `event lookup returns the generated descriptor`() {
        assertSame(Created.NATS_DESCRIPTOR, GeneratedDescriptors.event(Created::class.java))
    }

    @Test
    fun `service lookup returns the generated descriptor`() {
        assertSame(NotifierService.NATS_DESCRIPTOR, GeneratedDescriptors.service(NotifierService::class.java))
    }

    @Test
    fun `class without a descriptor is not an event`() {
        val message = assertFailsWith<IllegalArgumentException> {
            GeneratedDescriptors.event(NotAnEvent::class.java)
        }.message.orEmpty()

        assertContains(message, "${NotAnEvent::class.java.name} is not a generated surf-nats event")
    }

    @Test
    fun `inherited descriptor field is rejected`() {
        assertFailsWith<IllegalArgumentException> { GeneratedDescriptors.event(InheritedDescriptorEvent::class.java) }
    }

    @Test
    fun `descriptor of the other kind is rejected`() {
        assertFailsWith<IllegalArgumentException> { GeneratedDescriptors.event(NotifierService::class.java) }
        assertFailsWith<IllegalArgumentException> { GeneratedDescriptors.service(Created::class.java) }
    }

    @Test
    fun `implementation class is rejected with a hint to the generated interface`() {
        val message = assertFailsWith<IllegalArgumentException> {
            GeneratedDescriptors.service(NotifierImpl::class.java)
        }.message.orEmpty()

        assertContains(message, "${NotifierImpl::class.java.name} is not a generated surf-nats service interface")
        assertContains(message, "registerService<NotifierService>(implementation)")
        assertContains(message, NotifierService::class.java.name)
    }
}
