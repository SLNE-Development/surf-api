package dev.slne.surf.microservice.database.integration

import dev.slne.surf.microservice.api.health.MicroserviceHealthContributor
import dev.slne.surf.microservice.api.integration.MicroserviceIntegration
import dev.slne.surf.microservice.database.health.DatabaseHealthContributor
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceRegistrationTest {

    @Test
    fun `registers the database integration`() {
        val integration = ServiceLoader.load(MicroserviceIntegration::class.java).single { it is DatabaseIntegration }

        assertEquals("database", integration.name)
        assertEquals(MicroserviceIntegration.DATABASE_ORDER, integration.order)
    }

    @Test
    fun `registers the database health contributor`() {
        val contributors = ServiceLoader.load(MicroserviceHealthContributor::class.java)

        assertTrue(contributors.any { it is DatabaseHealthContributor })
    }
}
