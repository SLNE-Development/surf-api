package dev.slne.surf.microservice.database.health

import kotlinx.coroutines.runBlocking
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Reports whether [probe] completes within [timeout], 2.5 seconds by default.
 *
 * [health] blocks the calling thread until the probe finished and reacts to interruption, so the
 * caller can enforce its own deadline.
 */
internal class DatabaseHealthIndicator(
    private val timeout: Duration = DEFAULT_TIMEOUT,
    private val probe: suspend () -> Unit,
) : HealthIndicator {

    override fun health(): Health = runBlocking { checkDatabaseHealth(timeout, probe) }

    companion object {
        val DEFAULT_TIMEOUT = 2.5.seconds
    }
}
