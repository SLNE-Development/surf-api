package dev.slne.surf.microservice.runtime.health

import dev.slne.surf.microservice.api.health.MicroserviceHealthCheckResult
import dev.slne.surf.microservice.api.health.MicroserviceHealthContributor
import dev.slne.surf.microservice.api.health.MicroserviceHealthStatus
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class MicroserviceHealthManagerTest {

    @Test
    fun `sorts the results of all contributors by component and instance`() = runTest {
        val manager = MicroserviceHealthManager {
            listOf(
                contributor("nats") { listOf(healthy("nats")) },
                contributor("database") { listOf(healthy("database", "b"), healthy("database", "a")) },
            )
        }

        val results = manager.checkAll()

        assertEquals(listOf("database/a", "database/b", "nats/"), results.map { "${it.component}/${it.instance.orEmpty()}" })
    }

    @Test
    fun `reports a contributor that exceeds the timeout as unhealthy`() = runTest {
        val manager = MicroserviceHealthManager(contributorTimeout = 1.seconds) {
            listOf(contributor("slow") { awaitCancellation() })
        }

        val result = manager.checkAll().single()

        assertEquals("slow", result.component)
        assertEquals(MicroserviceHealthStatus.UNHEALTHY, result.status)
    }

    @Test
    fun `reports a throwing contributor as unhealthy without affecting the others`() = runTest {
        val manager = MicroserviceHealthManager {
            listOf(
                contributor("broken") { error("no connection") },
                contributor("nats") { listOf(healthy("nats")) },
            )
        }

        val results = manager.checkAll()

        assertEquals(listOf("broken" to MicroserviceHealthStatus.UNHEALTHY, "nats" to MicroserviceHealthStatus.HEALTHY), results.map { it.component to it.status })
        assertEquals("Health check failed: no connection", results.first().message)
    }

    @Test
    fun `reports a failure to load the contributors as unhealthy`() = runTest {
        val manager = MicroserviceHealthManager { error("bad service file") }

        val result = manager.checkAll().single()

        assertEquals("service-loader", result.component)
        assertEquals(MicroserviceHealthStatus.UNHEALTHY, result.status)
    }

    private fun contributor(contributorName: String, results: suspend () -> List<MicroserviceHealthCheckResult>) =
        object : MicroserviceHealthContributor {
            override val name = contributorName

            override suspend fun check() = results()
        }

    private fun healthy(component: String, instance: String? = null) =
        MicroserviceHealthCheckResult(component = component, instance = instance, status = MicroserviceHealthStatus.HEALTHY)
}
