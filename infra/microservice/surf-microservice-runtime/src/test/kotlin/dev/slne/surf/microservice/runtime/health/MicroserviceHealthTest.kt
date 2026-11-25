package dev.slne.surf.microservice.runtime.health

import kotlinx.coroutines.runBlocking
import org.springframework.boot.health.contributor.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class MicroserviceHealthTest {

    @Test
    fun `sorts the results by component and instance`() = runBlocking {
        val database = CompositeHealthContributor.fromMap(mapOf("b" to up(), "a" to up()))

        val results = health("nats" to up(), "database" to database).checkAll()

        assertEquals(listOf("database[a]", "database[b]", "nats"), results.map { it.displayName })
    }

    @Test
    fun `reports an indicator that exceeds the timeout as unhealthy`() = runBlocking {
        val slow = HealthIndicator { Thread.sleep(5_000); Health.up().build() }

        val health = MicroserviceHealth(
            contributors = { contributors("slow" to slow) },
            indicatorTimeout = 100.milliseconds,
        )

        val result = health.checkAll().single()

        assertFalse(result.isHealthy)
        assertEquals("Health check timed out after 100ms.", result.message)
    }

    @Test
    fun `reports a throwing indicator as unhealthy without affecting the others`() = runBlocking {
        val broken = HealthIndicator { error("no connection") }

        val results = health("broken" to broken, "nats" to up()).checkAll()

        assertEquals(
            listOf("broken" to false, "nats" to true),
            results.map { result -> result.component to result.isHealthy },
        )
        assertEquals("Health check failed: no connection", results.first().message)
    }

    @Test
    fun `a down indicator reports its error detail`() = runBlocking {
        val down = HealthIndicator {
            Health.down()
                .withDetail("error", "connection refused")
                .build()
        }

        val result = health("database" to down).checkAll().single()

        assertEquals("connection refused", result.message)
    }

    @Test
    fun `an unknown indicator is healthy`() = runBlocking {
        val unknown = HealthIndicator { Health.unknown().build() }

        val result = health("nats" to unknown).checkAll().single()

        assertTrue(result.isHealthy)
    }

    @Test
    fun `an out of service indicator is unhealthy`() = runBlocking {
        val outOfService = HealthIndicator { Health.outOfService().build() }

        val result = health("database" to outOfService).checkAll().single()

        assertFalse(result.isHealthy)
        assertEquals("OUT_OF_SERVICE", result.message)
    }

    @Test
    fun `no contributors means no results`() = runBlocking {
        assertTrue(MicroserviceHealth({ null }).checkAll().isEmpty())
    }

    private fun health(vararg contributors: Pair<String, HealthContributor>) = MicroserviceHealth(
        {
            contributors(
                *contributors,
            )
        },
    )

    private fun contributors(
        vararg contributors: Pair<String, HealthContributor>,
    ): HealthContributors {
        return CompositeHealthContributor.fromMap(contributors.toMap())
    }

    private fun up() = HealthIndicator { Health.up().build() }
}
