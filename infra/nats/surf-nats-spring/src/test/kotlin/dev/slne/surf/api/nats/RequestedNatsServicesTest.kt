package dev.slne.surf.api.nats

import dev.slne.surf.nats.test.proto.v1.NotifierService
import dev.slne.surf.nats.test.proto.v1.OrderService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import java.util.Optional
import kotlin.test.assertEquals

@Suppress("unused", "UNUSED_PARAMETER")
class RequestedNatsServicesTest {

    private val notifier = setOf<Class<*>>(NotifierService::class.java)

    private fun requestedBy(type: Class<*>) = RequestedNatsServices.requestedBy(type)

    class ByConstructor(val notifier: NotifierService)

    class ByNullableConstructor(val notifier: NotifierService?)

    class ByDefaultedConstructor(val notifier: NotifierService, val count: Int = 3)

    class ByAutowiredConstructor {
        constructor(orders: OrderService)

        @Autowired
        constructor(notifier: NotifierService)
    }

    class ByField {
        @Autowired
        lateinit var notifier: NotifierService
    }

    class BySetter {
        @Autowired
        fun configure(notifier: NotifierService, name: String) = Unit
    }

    open class FieldHolder {
        @Autowired
        lateinit var notifier: NotifierService
    }

    class BySuperclassField : FieldHolder()

    abstract class GenericHolder<S : Any> {
        @Autowired
        lateinit var service: S

        @Autowired
        fun configure(service: S) = Unit
    }

    class BoundGenericHolder : GenericHolder<NotifierService>()

    class UnboundGenericHolder<S : Any> : GenericHolder<S>()

    class ByBeanMethod {
        @Bean
        fun pool(notifier: NotifierService): Any = Any()
    }

    class ByObjectProvider(val notifier: ObjectProvider<NotifierService>)

    class ByWildcardObjectProvider(val notifier: ObjectProvider<out NotifierService>)

    class ByObjectFactory(val notifier: ObjectFactory<NotifierService>)

    class ByOptional(val notifier: Optional<NotifierService>)

    class ByNothing(val name: String, val list: List<NotifierService>)

    class UnannotatedMembers {
        lateinit var notifier: NotifierService

        fun configure(notifier: NotifierService) = Unit
    }

    class Two(val notifier: NotifierService, val orders: OrderService)

    @Test
    fun `constructor parameter`() = assertEquals(notifier, requestedBy(ByConstructor::class.java))

    @Test
    fun `nullable Kotlin constructor parameter`() =
        assertEquals(notifier, requestedBy(ByNullableConstructor::class.java))

    @Test
    fun `Kotlin primary constructor with a default value`() =
        assertEquals(notifier, requestedBy(ByDefaultedConstructor::class.java))

    @Test
    fun `only the constructor Spring uses counts`() = assertEquals(notifier, requestedBy(ByAutowiredConstructor::class.java))

    @Test
    fun `autowired field`() = assertEquals(notifier, requestedBy(ByField::class.java))

    @Test
    fun `autowired setter`() = assertEquals(notifier, requestedBy(BySetter::class.java))

    @Test
    fun `autowired field of a superclass`() = assertEquals(notifier, requestedBy(BySuperclassField::class.java))


    @Test
    fun `generic members of a superclass resolve against the bean class`() =
        assertEquals(notifier, requestedBy(BoundGenericHolder::class.java))

    @Test
    fun `a generic member that the bean class leaves unbound is not reported`() =
        assertEquals(emptySet(), requestedBy(UnboundGenericHolder::class.java))

    @Test
    fun `the constructors of a factory method's result are skipped on request`() {
        assertEquals(emptySet(), RequestedNatsServices.requestedBy(ByConstructor::class.java, includeConstructors = false))
        assertEquals(notifier, RequestedNatsServices.requestedBy(ByField::class.java, includeConstructors = false))
    }
    @Test
    fun `parameter of a bean method`() = assertEquals(notifier, requestedBy(ByBeanMethod::class.java))

    @Test
    fun `ObjectProvider type argument`() = assertEquals(notifier, requestedBy(ByObjectProvider::class.java))

    @Test
    fun `wildcard ObjectProvider type argument`() =
        assertEquals(notifier, requestedBy(ByWildcardObjectProvider::class.java))

    @Test
    fun `ObjectFactory type argument`() = assertEquals(notifier, requestedBy(ByObjectFactory::class.java))

    @Test
    fun `Optional type argument`() = assertEquals(notifier, requestedBy(ByOptional::class.java))

    @Test
    fun `several service interfaces are all reported`() = assertEquals(
        setOf(NotifierService::class.java, OrderService::class.java),
        requestedBy(Two::class.java),
    )

    @Test
    fun `other types and collections are not reported`() =
        assertEquals(emptySet(), requestedBy(ByNothing::class.java))

    @Test
    fun `members without an injection annotation are not reported`() =
        assertEquals(emptySet(), requestedBy(UnannotatedMembers::class.java))
}
