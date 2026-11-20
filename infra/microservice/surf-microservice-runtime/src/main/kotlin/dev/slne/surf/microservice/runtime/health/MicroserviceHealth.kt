package dev.slne.surf.microservice.runtime.health

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import org.springframework.boot.health.contributor.CompositeHealthContributor
import org.springframework.boot.health.contributor.HealthContributors
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.boot.health.contributor.Status
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runs every health indicator of the context concurrently, each within [indicatorTimeout].
 *
 * A top-level indicator yields one result named after it; every indicator of a composite contributor yields one
 * result named after the composite, with its own name as the instance. An indicator that throws or exceeds
 * [indicatorTimeout] is unhealthy. Reactive contributors are not checked.
 *
 * @param contributors the context's health contributors, or `null` while there are none
 */
internal class MicroserviceHealth(
    private val contributors: () -> HealthContributors?,
    private val indicatorTimeout: Duration = DEFAULT_INDICATOR_TIMEOUT,
) {

    /**
     * Returns the results of all indicators, sorted by component and instance; empty without any indicator.
     */
    suspend fun checkAll(): List<HealthCheckResult> {
        val indicators = contributors()?.let(::indicatorsOf).orEmpty()
        val results = supervisorScope {
            indicators
                .map { indicator -> async(CoroutineName("health-${indicator.component}")) { check(indicator) } }
                .awaitAll()
        }
        return results.sortedWith(compareBy({ it.component }, { it.instance.orEmpty() }))
    }

    private suspend fun check(indicator: NamedIndicator): HealthCheckResult = try {
        val health = withTimeout(indicatorTimeout) {
            runInterruptible(Dispatchers.IO) { indicator.indicator.health(true) }
        }
        when {
            health == null -> indicator.result(healthy = false, message = "The indicator reported no health.")
            health.status == Status.UP -> indicator.result(healthy = true, message = null)
            else -> indicator.result(healthy = false, message = describe(health.status, health.details))
        }
    } catch (_: TimeoutCancellationException) {
        indicator.result(healthy = false, message = "Health check timed out after $indicatorTimeout.")
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        indicator.result(healthy = false, message = "Health check failed: ${exception.describe()}")
    }

    private fun describe(status: Status, details: Map<String, Any?>): String =
        details["error"]?.toString() ?: status.description.ifBlank { status.code }

    private fun Throwable.describe(): String = message?.ifBlank { null } ?: javaClass.name

    private class NamedIndicator(val component: String, val instance: String?, val indicator: HealthIndicator) {
        fun result(healthy: Boolean, message: String?) = HealthCheckResult(component, instance, healthy, message)
    }

    private companion object {
        val DEFAULT_INDICATOR_TIMEOUT = 3.seconds

        fun indicatorsOf(contributors: HealthContributors): List<NamedIndicator> = buildList {
            for (entry in contributors) {
                when (val contributor = entry.contributor()) {
                    is HealthIndicator -> add(NamedIndicator(entry.name(), null, contributor))
                    is CompositeHealthContributor -> contributor.forEach { child ->
                        val indicator = child.contributor()
                        if (indicator is HealthIndicator) add(NamedIndicator(entry.name(), child.name(), indicator))
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
    val healthy: Boolean,
    val message: String?,
) {
    val displayName: String get() = if (instance == null) component else "$component[$instance]"
}
