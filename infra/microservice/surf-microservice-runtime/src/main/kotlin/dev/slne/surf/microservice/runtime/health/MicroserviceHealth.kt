package dev.slne.surf.microservice.runtime.health

import kotlinx.coroutines.*
import org.springframework.boot.health.contributor.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runs every health indicator of the context concurrently, each within [indicatorTimeout].
 *
 * A top-level indicator yields one result named after it; every indicator of a composite
 * contributor yields one result named after the composite, with its own name as the instance.
 *
 * As in Spring Boot's aggregation, only `DOWN` and `OUT_OF_SERVICE` are unhealthy; `UNKNOWN` and
 * custom statuses count as healthy. An indicator that reports no health, throws or exceeds
 * [indicatorTimeout] is unhealthy as well. Reactive contributors are not checked.
 *
 * @param contributors the context's health contributors, or `null` while there are none
 */
internal class MicroserviceHealth(
    private val contributors: () -> HealthContributors?,
    private val indicatorTimeout: Duration = DEFAULT_INDICATOR_TIMEOUT,
) {

    /**
     * Returns the results of all indicators, sorted by component and instance.
     *
     * @return the results, or an empty list without any indicator
     */
    suspend fun checkAll(): List<HealthCheckResult> {
        val indicators = contributors()?.let(::indicatorsOf).orEmpty()
        val results = supervisorScope {
            indicators
                .map { indicator ->
                    async(CoroutineName("health-${indicator.component}")) { check(indicator) }
                }
                .awaitAll()
        }

        return results.sortedWith(compareBy({ it.component }, { it.instance.orEmpty() }))
    }

    private suspend fun check(indicator: NamedIndicator): HealthCheckResult {
        return try {
            val health = withTimeout(indicatorTimeout) {
                runInterruptible(Dispatchers.IO) { indicator.indicator.health(true) }
            }

            when {
                health == null -> indicator.unhealthy("The indicator reported no health.")
                health.status in UNHEALTHY_STATUSES -> indicator.unhealthy(describe(health))
                else -> indicator.healthy()
            }
        } catch (_: TimeoutCancellationException) {
            indicator.unhealthy("Health check timed out after $indicatorTimeout.")
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            indicator.unhealthy("Health check failed: ${exception.describe()}")
        }
    }

    private fun describe(health: Health): String {
        return health.details["error"]?.toString()
            ?: health.status.description.ifBlank { health.status.code }
    }

    private fun Throwable.describe(): String = message?.ifBlank { null } ?: javaClass.name

    private class NamedIndicator(
        val component: String,
        val instance: String?,
        val indicator: HealthIndicator,
    ) {
        fun healthy(): HealthCheckResult {
            return HealthCheckResult(component, instance, isHealthy = true, message = null)
        }

        fun unhealthy(message: String): HealthCheckResult {
            return HealthCheckResult(component, instance, isHealthy = false, message = message)
        }
    }

    private companion object {
        val DEFAULT_INDICATOR_TIMEOUT = 3.seconds
        val UNHEALTHY_STATUSES = setOf(Status.DOWN, Status.OUT_OF_SERVICE)

        fun indicatorsOf(contributors: HealthContributors): List<NamedIndicator> {
            return buildList {
                for (entry in contributors) {
                    when (val contributor = entry.contributor()) {
                        is HealthIndicator -> add(NamedIndicator(entry.name(), null, contributor))
                        is CompositeHealthContributor -> {
                            for (child in contributor) {
                                val indicator = child.contributor()

                                if (indicator is HealthIndicator) {
                                    add(NamedIndicator(entry.name(), child.name(), indicator))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The result of one health indicator.
 *
 * @property message why the check failed; `null` for a healthy result
 */
internal data class HealthCheckResult(
    val component: String,
    val instance: String?,
    val isHealthy: Boolean,
    val message: String?,
) {
    val displayName: String get() = if (instance == null) component else "$component[$instance]"
}
