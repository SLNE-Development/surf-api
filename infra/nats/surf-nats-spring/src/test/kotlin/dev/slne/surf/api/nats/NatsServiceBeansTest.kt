package dev.slne.surf.api.nats

import dev.slne.surf.nats.NatsService
import dev.slne.surf.nats.NatsTransactional
import dev.slne.surf.nats.event.NatsListener
import dev.slne.surf.nats.test.proto.v1.Created
import dev.slne.surf.nats.test.proto.v1.NotifierService
import dev.slne.surf.nats.test.proto.v1.OrderService
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The classes are abstract: validation reads class metadata only.
 */
class NatsServiceBeansTest {

    private fun served(type: Class<*>) = NatsServiceBeans.servedInterfaces("bean", type)

    @NatsService
    abstract class AllInterfaces : NotifierService

    @NatsService
    abstract class Both : NotifierService, OrderService

    @NatsService(NotifierService::class)
    abstract class Narrowed : NotifierService, OrderService

    @NatsService
    abstract class NoInterface

    @NatsService(OrderService::class)
    abstract class ListsUnimplemented : NotifierService

    @NatsService(Runnable::class)
    abstract class ListsNonService : NotifierService, Runnable

    abstract class Unannotated : NotifierService

    @NatsTransactional
    abstract class TransactionalOnly

    abstract class InheritsAnnotation : AllInterfaces()

    abstract class ListenerBase {
        @NatsListener
        fun on(event: Created) = Unit
    }

    abstract class ListenerDerived : ListenerBase()

    @Test
    fun `a class without the annotation serves nothing`() = assertNull(served(Unannotated::class.java))

    @Test
    fun `without a value the bean serves every generated interface it implements`() {
        assertEquals(listOf<Class<*>>(NotifierService::class.java), served(AllInterfaces::class.java))
        assertEquals(
            setOf<Class<*>>(NotifierService::class.java, OrderService::class.java),
            served(Both::class.java)?.toSet(),
        )
    }

    @Test
    fun `a value narrows the served interfaces`() {
        assertEquals(listOf<Class<*>>(NotifierService::class.java), served(Narrowed::class.java))
    }

    @Test
    fun `the annotation of a superclass applies`() {
        assertEquals(listOf<Class<*>>(NotifierService::class.java), served(InheritsAnnotation::class.java))
    }

    @Test
    fun `a class that implements no generated interface is rejected`() {
        val failure = assertFailsWith<IllegalStateException> { served(NoInterface::class.java) }

        assertContains(failure.message.orEmpty(), "implements no generated NATS service interface")
    }

    @Test
    fun `a listed interface that the class does not implement is rejected`() {
        val failure = assertFailsWith<IllegalStateException> { served(ListsUnimplemented::class.java) }

        assertContains(failure.message.orEmpty(), "does not implement")
    }

    @Test
    fun `a listed type that is not a generated interface is rejected`() {
        val failure = assertFailsWith<IllegalStateException> { served(ListsNonService::class.java) }

        assertContains(failure.message.orEmpty(), "not a generated NATS service interface")
    }

    @Test
    fun `a class takes part in NATS when it serves, listens or is transactional`() {
        assertTrue(NatsServiceBeans.isNatsComponent(AllInterfaces::class.java))
        assertTrue(NatsServiceBeans.isNatsComponent(InheritsAnnotation::class.java))
        assertTrue(NatsServiceBeans.isNatsComponent(ListenerDerived::class.java))
        assertTrue(NatsServiceBeans.isNatsComponent(TransactionalOnly::class.java))
        assertFalse(NatsServiceBeans.isNatsComponent(Unannotated::class.java))
    }

    @Test
    fun `usesNats reads annotations without validating the service`() {
        assertTrue(NatsServiceBeans.isNatsComponent(NoInterface::class.java))
    }

    @Test
    fun `a class that cannot be reflected because of a missing class does not use NATS`() {
        val type = HidingClassLoader(SignatureNeedsHidden::class.java).loadClass(SignatureNeedsHidden::class.java.name)
        assertFailsWith<NoClassDefFoundError> { type.declaredMethods }

        assertFalse(NatsServiceBeans.isNatsComponent(type))
    }

    @Test
    fun `listener methods are found in the class and its superclasses`() {
        assertTrue(NatsServiceBeans.hasListenerMethods(ListenerDerived::class.java))
        assertFalse(NatsServiceBeans.hasListenerMethods(Unannotated::class.java))
    }
}
