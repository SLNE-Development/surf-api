package dev.slne.surf.microservice.database.health

import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.boot.health.contributor.Health
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * Runs [probe] with [timeout] and reports the outcome as a [Health].
 *
 * A timeout or any [Throwable] from [probe], [Error]s included, yields a down status with the reason in the `error`
 * detail. Cancellation of the caller propagates.
 */
internal suspend fun checkDatabaseHealth(timeout: Duration, probe: suspend () -> Unit): Health {
    val completed = try {
        withTimeoutOrNull(timeout) { probe() } != null
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Throwable) {
        return down(exception.message?.takeIf(String::isNotBlank) ?: exception.javaClass.name)
    }

    return if (completed) Health.up().build() else down("Database health check timed out after $timeout")
}

private fun down(error: String): Health = Health.down().withDetail("error", error).build()
