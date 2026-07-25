package dev.slne.surf.microservice.runtime.microservice.health

import dev.slne.surf.microservice.api.microservice.health.MicroserviceHealthCheckResult
import dev.slne.surf.microservice.api.microservice.health.MicroserviceHealthContributor
import dev.slne.surf.microservice.api.microservice.health.MicroserviceHealthStatus
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

internal object MicroserviceHealthManager {

    private val CONTRIBUTOR_TIMEOUT = 3.seconds

    private val contributorsResult by lazy {
        runCatching {
            ServiceLoader.load(
                MicroserviceHealthContributor::class.java,
                MicroserviceHealthContributor::class.java.classLoader
            )
                .toList()
                .sortedBy { contributor -> contributor.name }
        }
    }

    suspend fun checkAll(): List<MicroserviceHealthCheckResult> {
        val contributors = contributorsResult.getOrElse { exception ->
            return listOf(
                unhealthyResult(
                    component = "service-loader",
                    message = "Could not load health contributors: ${exception.describe()}"
                )
            )
        }

        return supervisorScope {
            contributors
                .map { contributor ->
                    async(
                        CoroutineName(
                            "MicroserviceHealthContributor-${contributor.safeName()}"
                        )
                    ) {
                        checkContributor(contributor)
                    }
                }
                .awaitAll()
                .flatten()
        }.sortedWith(
            compareBy(
                MicroserviceHealthCheckResult::component,
                { result -> result.instance.orEmpty() }
            )
        )
    }

    private suspend fun checkContributor(
        contributor: MicroserviceHealthContributor
    ): List<MicroserviceHealthCheckResult> {
        val contributorName = contributor.safeName()

        return try {
            withTimeout(CONTRIBUTOR_TIMEOUT) {
                contributor.check()
            }
        } catch (_: TimeoutCancellationException) {
            listOf(
                unhealthyResult(
                    component = contributorName,
                    message = "Health check timed out after $CONTRIBUTOR_TIMEOUT."
                )
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            listOf(
                unhealthyResult(
                    component = contributorName,
                    message = "Health check failed: ${exception.describe()}"
                )
            )
        }
    }

    private fun MicroserviceHealthContributor.safeName(): String {
        return runCatching { name.trim() }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: javaClass.name
    }

    private fun unhealthyResult(
        component: String,
        message: String
    ) = MicroserviceHealthCheckResult(
        component = component,
        status = MicroserviceHealthStatus.UNHEALTHY,
        message = message
    )

    private fun Throwable.describe(): String {
        return message
            ?.takeIf { it.isNotBlank() }
            ?: javaClass.name
    }
}