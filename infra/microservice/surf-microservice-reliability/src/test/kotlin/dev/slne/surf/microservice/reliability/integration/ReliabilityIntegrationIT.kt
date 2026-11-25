@file:OptIn(InternalNatsApi::class)

package dev.slne.surf.microservice.reliability.integration

import dev.slne.surf.database.reliability.operation.AtomicOperations
import dev.slne.surf.microservice.api.MicroserviceInfo
import dev.slne.surf.microservice.reliability.MicroserviceReliabilityAutoConfiguration
import dev.slne.surf.microservice.reliability.ReliabilityConfig
import dev.slne.surf.microservice.reliability.TransactionalEvents
import dev.slne.surf.microservice.reliability.operation.StoredReplyCleanup
import dev.slne.surf.microservice.reliability.operation.TransactionalInterceptor
import dev.slne.surf.microservice.reliability.support.OpenedDatabase
import dev.slne.surf.microservice.reliability.support.TestVendor
import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.NatsServiceInterceptorProvider
import dev.slne.surf.nats.NatsTransactional
import io.mockk.mockk
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeansOfType
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.support.registerBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Path
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes

/**
 * [MicroserviceReliabilityAutoConfiguration] in a context with a microservice identity and a
 * database, on both supported vendors.
 */
@Testcontainers(disabledWithoutDocker = true)
class ReliabilityIntegrationIT {

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `creates the stored reply table and the beans of the handlers`(vendor: TestVendor) {
        reliable(vendor).use { opened ->
            assertEquals(0L, runBlocking { storedReplyCount(opened.database) })
            assertNotNull(opened.context.getBean<AtomicOperations>())
            assertNotNull(opened.context.getBean<TransactionalEvents>())
            assertEquals(ReliabilityConfig(), opened.context.getBean<ReliabilityConfig>())
        }
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `the provider wraps only the beans of a transactional service`(vendor: TestVendor) {
        reliable(vendor).use { opened ->
            val provider = opened.context.getBean<NatsServiceInterceptorProvider>()
            val client = mockk<NatsClient>()

            assertIs<TransactionalInterceptor>(
                provider.interceptorFor(
                    TransactionalService(),
                    TransactionalService::class.java,
                    client,
                ),
            )
            assertIs<TransactionalInterceptor>(
                provider.interceptorFor(InheritingService(), InheritingService::class.java, client),
            )
            assertNull(provider.interceptorFor(PlainService(), PlainService::class.java, client))
        }
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `the cleanup runs while the context runs`(vendor: TestVendor) {
        val opened = reliable(vendor)
        val cleanup = opened.context.getBean<StoredReplyCleanup>()

        try {
            assertTrue(cleanup.isRunning, "the cleanup must start with the context")

            opened.context.stop()
            assertFalse(cleanup.isRunning, "the cleanup must stop with the context")

            opened.context.start()
            assertTrue(cleanup.isRunning, "the cleanup must start again with the context")
        } finally {
            opened.close()
        }

        assertFalse(cleanup.isRunning, "the cleanup must not outlive the closed context")
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `an application config bean replaces the default`(vendor: TestVendor) {
        val applicationConfig = ReliabilityConfig(cleanupInterval = 5.minutes)

        reliable(vendor) { registerBean<ReliabilityConfig> { applicationConfig } }.use { opened ->
            assertEquals(1, opened.context.getBeansOfType<ReliabilityConfig>().size)
            assertSame(applicationConfig, opened.context.getBean<ReliabilityConfig>())
        }
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `backs off without a microservice identity`(vendor: TestVendor) {
        vendor.freshDatabase().open("no-identity") {
            register(MicroserviceReliabilityAutoConfiguration::class.java)
        }.use { opened ->
            assertTrue(opened.context.getBeansOfType<AtomicOperations>().isEmpty())
            assertTrue(opened.context.getBeansOfType<TransactionalEvents>().isEmpty())
            assertTrue(opened.context.getBeansOfType<NatsServiceInterceptorProvider>().isEmpty())
        }
    }

    private fun reliable(
        vendor: TestVendor,
        configure: AnnotationConfigApplicationContext.() -> Unit = {},
    ): OpenedDatabase = vendor.freshDatabase().open("reliability-integration") {
        beanFactory.registerSingleton(
            "microserviceInfo",
            MicroserviceInfo(SERVICE_NAME, Path.of("unused")),
        )
        configure()
        register(MicroserviceReliabilityAutoConfiguration::class.java)
    }

    private suspend fun storedReplyCount(database: R2dbcDatabase): Long {
        return suspendTransaction(db = database) {
            val counts = exec("SELECT COUNT(*) FROM surf_atomic_operations") { row ->
                (row.get(0) as Number).toLong()
            }
            counts!!.single()!!
        }
    }

    @NatsTransactional
    private open class TransactionalService

    private class InheritingService : TransactionalService()

    private class PlainService

    private companion object {
        const val SERVICE_NAME = "reliability-integration"
    }
}
