package dev.slne.surf.microservice.database.health

import dev.slne.surf.microservice.api.health.MicroserviceHealthCheckResult
import dev.slne.surf.microservice.api.health.MicroserviceHealthStatus
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * Runs [probe] with [timeout] and reports the outcome as [component].
 *
 * A timeout or any [Throwable] from [probe], [Error]s included, yields [MicroserviceHealthStatus.UNHEALTHY];
 * cancellation of the caller propagates.
 */
internal suspend fun checkDatabaseHealth(
    component: String,
    timeout: Duration,
    probe: suspend () -> Unit,
): MicroserviceHealthCheckResult {
    val completed = try {
        withTimeoutOrNull(timeout) { probe() } != null
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Throwable) {
        return unhealthy(component, exception.message?.takeIf(String::isNotBlank) ?: exception.javaClass.name)
    }

    return if (completed) {
        MicroserviceHealthCheckResult(component = component, status = MicroserviceHealthStatus.HEALTHY)
    } else {
        unhealthy(component, "Database health check timed out after $timeout")
    }
}

private fun unhealthy(component: String, message: String) = MicroserviceHealthCheckResult(
    component = component,
    status = MicroserviceHealthStatus.UNHEALTHY,
    message = message,
)
