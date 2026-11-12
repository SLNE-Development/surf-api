package dev.slne.surf.microservice.database.health

import com.google.auto.service.AutoService
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.microservice.api.health.MicroserviceHealthCheckResult
import dev.slne.surf.microservice.api.health.MicroserviceHealthContributor
import dev.slne.surf.microservice.database.integration.DatabaseHolder
import kotlin.time.Duration.Companion.seconds

/**
 * Reports whether the database answers `SELECT 1` within 2.5 seconds.
 *
 * Reports nothing while the database integration is not running.
 */
@AutoService(MicroserviceHealthContributor::class)
internal class DatabaseHealthContributor : MicroserviceHealthContributor {
    override val name = "database"

    override suspend fun check(): List<MicroserviceHealthCheckResult> {
        val api = DatabaseHolder.currentOrNull() ?: return emptyList()

        return listOf(
            checkDatabaseHealth(name, TIMEOUT) {
                suspendTransaction(db = api.database) { exec("SELECT 1") }
            }
        )
    }

    private companion object {
        val TIMEOUT = 2.5.seconds
    }
}
