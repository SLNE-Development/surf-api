package dev.slne.surf.microservice.reliability

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import dev.slne.surf.database.reliability.operation.AtomicOperations
import dev.slne.surf.microservice.api.MicroserviceInfo
import dev.slne.surf.nats.NatsServiceInterceptorProvider
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeanProvider
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.registerBean
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Checks how [MicroserviceReliabilityAutoConfiguration] backs off for beans of the application. The beans of the
 * application are mocks, so nothing here connects to a database.
 */
class MicroserviceReliabilityAutoConfigurationTest {

    private val contexts = mutableListOf<AnnotationConfigApplicationContext>()

    @AfterEach
    fun closeContexts() = contexts.forEach(AnnotationConfigApplicationContext::close)

    @Test
    fun `an application AtomicOperations bean replaces the default`() {
        val operations = mockk<AtomicOperations>()

        val context = context { registerBean<AtomicOperations>("invoiceOperations") { operations } }

        assertEquals(1, context.getBeansOfType<AtomicOperations>().size)
        assertSame(operations, context.getBean<AtomicOperations>())
    }

    @Test
    fun `an application AtomicOperations bean named like the default replaces it`() {
        val operations = mockk<AtomicOperations>()

        val context = context { registerBean<AtomicOperations>("atomicOperations") { operations } }

        assertSame(operations, context.getBean<AtomicOperations>("atomicOperations"))
    }

    @Test
    fun `an application TransactionalEvents bean replaces the default`() {
        val events = TransactionalEvents()

        val context = context {
            registerBean<AtomicOperations> { mockk() }
            registerBean<TransactionalEvents>("applicationEvents") { events }
        }

        assertEquals(1, context.getBeansOfType<TransactionalEvents>().size)
        assertSame(events, context.getBean<TransactionalEvents>())
    }

    @Test
    fun `the interceptor provider declares it handles transactional beans and comes first`() {
        val generic = NatsServiceInterceptorProvider { _, _, _ -> null }

        val context = context {
            registerBean<AtomicOperations> { mockk() }
            registerBean<NatsServiceInterceptorProvider>("genericProvider") { generic }
        }

        val providers = context.getBeanProvider<NatsServiceInterceptorProvider>().orderedStream().toList()
        assertIs<TransactionalInterceptorProvider>(providers.first())
        assertTrue(providers.first().handlesTransactional)
        assertFalse(generic.handlesTransactional)
    }

    private fun context(configure: AnnotationConfigApplicationContext.() -> Unit): AnnotationConfigApplicationContext {
        val context = AnnotationConfigApplicationContext()
        contexts += context
        context.beanFactory.registerSingleton("microserviceInfo", MicroserviceInfo("reliability-test", Path.of("unused")))
        context.registerBean<R2dbcDatabase> { mockk() }
        context.configure()
        context.register(MicroserviceReliabilityAutoConfiguration::class.java)
        context.refresh()
        return context
    }
}
