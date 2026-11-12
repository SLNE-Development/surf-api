package dev.slne.surf.microservice.runtime.health

import dev.slne.surf.microservice.api.health.MicroserviceHealthCheckResult
import dev.slne.surf.microservice.api.health.MicroserviceHealthContributor
import dev.slne.surf.microservice.api.health.MicroserviceHealthStatus
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Runs the checks of all health contributors concurrently.
 *
 * [loadContributors] runs once, on the first [checkAll]. A failure to load is reported as an unhealthy result on
 * every call. A contributor that throws or exceeds [contributorTimeout] yields one unhealthy result named after it.
 */
internal class MicroserviceHealthManager(
    private val contributorTimeout: Duration = DEFAULT_CONTRIBUTOR_TIMEOUT,
    loadContributors: () -> List<MicroserviceHealthContributor> = ::loadServiceContributors,
) {
    private val contributors by lazy {
        runCatching { loadContributors().sortedBy { it.safeName() } }
    }

    /**
     * Returns the results of all contributors, sorted by component and instance.
     */
    suspend fun checkAll(): List<MicroserviceHealthCheckResult> {
        val contributors = contributors.getOrElse { exception ->
            return listOf(
                unhealthy(
                    "service-loader",
                    "Could not load health contributors: ${exception.describe()}"
                )
            )
        }

        val results = supervisorScope {
            contributors
                .map { contributor ->
                    async(CoroutineName("MicroserviceHealthContributor-${contributor.safeName()}")) {
                        check(contributor)
                    }
                }
                .awaitAll()
                .flatten()
        }

        return results.sortedWith(compareBy({ it.component }, { it.instance.orEmpty() }))
    }

    private suspend fun check(contributor: MicroserviceHealthContributor): List<MicroserviceHealthCheckResult> {
        val name = contributor.safeName()
        return try {
            withTimeout(contributorTimeout) { contributor.check() }
        } catch (_: TimeoutCancellationException) {
            listOf(unhealthy(name, "Health check timed out after $contributorTimeout."))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            listOf(unhealthy(name, "Health check failed: ${exception.describe()}"))
        }
    }

    private fun MicroserviceHealthContributor.safeName(): String {
        return runCatching { name.trim() }
            .getOrNull()
            ?.ifEmpty { null }
            ?: javaClass.name
    }

    private fun unhealthy(
        component: String,
        message: String,
    ) = MicroserviceHealthCheckResult(
        component = component,
        status = MicroserviceHealthStatus.UNHEALTHY,
        message = message
    )

    private fun Throwable.describe(): String = message?.ifBlank { null } ?: javaClass.name

    private companion object {
        val DEFAULT_CONTRIBUTOR_TIMEOUT = 3.seconds

        fun loadServiceContributors(): List<MicroserviceHealthContributor> =
            ServiceLoader.load(
                MicroserviceHealthContributor::class.java,
                MicroserviceHealthContributor::class.java.classLoader
            ).toList()
    }
}
