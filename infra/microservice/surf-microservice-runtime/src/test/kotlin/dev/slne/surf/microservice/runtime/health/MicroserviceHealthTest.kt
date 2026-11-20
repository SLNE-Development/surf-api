package dev.slne.surf.microservice.runtime.health

import kotlinx.coroutines.runBlocking
import org.springframework.boot.health.contributor.CompositeHealthContributor
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthContributor
import org.springframework.boot.health.contributor.HealthContributors
import org.springframework.boot.health.contributor.HealthIndicator
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

        val result = MicroserviceHealth({ contributors("slow" to slow) }, indicatorTimeout = 100.milliseconds)
            .checkAll().single()

        assertFalse(result.healthy)
        assertEquals("Health check timed out after 100ms.", result.message)
    }

    @Test
    fun `reports a throwing indicator as unhealthy without affecting the others`() = runBlocking {
        val broken = HealthIndicator { error("no connection") }

        val results = health("broken" to broken, "nats" to up()).checkAll()

        assertEquals(listOf("broken" to false, "nats" to true), results.map { it.component to it.healthy })
        assertEquals("Health check failed: no connection", results.first().message)
    }

    @Test
    fun `a down indicator reports its error detail`() = runBlocking {
        val down = HealthIndicator { Health.down().withDetail("error", "connection refused").build() }

        val result = health("database" to down).checkAll().single()

        assertEquals("connection refused", result.message)
    }

    @Test
    fun `no contributors means no results`() = runBlocking {
        assertTrue(MicroserviceHealth({ null }).checkAll().isEmpty())
    }

    private fun health(vararg contributors: Pair<String, HealthContributor>) =
        MicroserviceHealth({ contributors(*contributors) })

    private fun contributors(vararg contributors: Pair<String, HealthContributor>): HealthContributors =
        CompositeHealthContributor.fromMap(contributors.toMap())

    private fun up() = HealthIndicator { Health.up().build() }
}
