package dev.slne.surf.microservice.reliability.integration

import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.api.integration.MicroserviceIntegration
import dev.slne.surf.microservice.api.integration.MicroserviceIntegrationContext
import dev.slne.surf.microservice.reliability.ReliabilityConfig
import dev.slne.surf.microservice.reliability.ReliabilityCustomizer
import dev.slne.surf.microservice.reliability.events
import dev.slne.surf.microservice.reliability.registerTransactionalService
import dev.slne.surf.microservice.reliability.support.ReceivedEvents
import dev.slne.surf.microservice.reliability.support.RecordingTestService
import dev.slne.surf.microservice.reliability.support.TestClients
import dev.slne.surf.microservice.reliability.support.TestNats
import dev.slne.surf.microservice.reliability.support.TestVendor
import dev.slne.surf.microservice.reliability.support.eventually
import dev.slne.surf.nats.service
import dev.slne.surf.nats.test.common.proto.v1.MicroserviceTestService
import dev.slne.surf.nats.test.common.proto.v1.StoreRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * The reliability integration as the microservice runtime drives it: migrate and start after the database and NATS,
 * serve transactional handlers from `onBootstrap`, stop after `onDisable`.
 */
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReliabilityIntegrationIT {

    private class ReliableMicroservice : Microservice(), ReliabilityCustomizer {
        override val name = SERVICE_NAME
        override val reliabilityConfig = ReliabilityConfig(cleanupInterval = 1.hours)
    }

    private class Context(override val microservice: Microservice) : MicroserviceIntegrationContext {
        override val name = microservice.name
        override val dataPath: Path = Path("unused")
    }

    @BeforeAll
    fun connect() = runBlocking { TestNats.start() }

    @AfterAll
    fun disconnect() = runBlocking { TestNats.stop() }

    @Test
    fun `starts after the database and messaging integrations and stops before them`() {
        val integration = ReliabilityIntegration()

        assertTrue(integration.order > MicroserviceIntegration.MESSAGING_ORDER)
        assertTrue(MicroserviceIntegration.MESSAGING_ORDER > MicroserviceIntegration.DATABASE_ORDER)
    }

    @ParameterizedTest
    @EnumSource(TestVendor::class)
    fun `migrates, serves transactional handlers and stops`(vendor: TestVendor) = runBlocking<Unit> {
        vendor.freshDatabase(migrateReliability = false).open("integration").use { api ->
            val microservice = ReliableMicroservice()
            val context = Context(microservice)
            val integration = ReliabilityIntegration { api }
            val serviceClient = TestClients.create("service")
            val caller = TestClients.create("caller")
            val received = ReceivedEvents()

            integration.start(context)
            try {
                caller.registerListener(received)
                serviceClient.registerTransactionalService<MicroserviceTestService>(
                    RecordingTestService(api, microservice.events)
                )

                caller.service<MicroserviceTestService>().store(StoreRequest("integration-entry"))

                eventually("the event must arrive") { received.messages == listOf("integration-entry") }
            } finally {
                withContext(Dispatchers.IO) { serviceClient.close() }
                integration.stop(context)
                withContext(Dispatchers.IO) { caller.close() }
            }

            assertFailsWith<IllegalStateException> { microservice.events }
        }
    }

    private companion object {
        const val SERVICE_NAME = "reliability-integration"
    }
}
