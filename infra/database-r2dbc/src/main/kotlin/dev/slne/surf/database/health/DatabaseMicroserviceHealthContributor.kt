package dev.slne.surf.database.health

import com.google.auto.service.AutoService
import dev.slne.surf.microservice.api.microservice.health.MicroserviceHealthCheckResult
import dev.slne.surf.microservice.api.microservice.health.MicroserviceHealthContributor
import dev.slne.surf.microservice.api.microservice.health.MicroserviceHealthStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

private val DATABASE_HEALTH_TIMEOUT = 2.5.seconds

@AutoService(MicroserviceHealthContributor::class)
internal class DatabaseMicroserviceHealthContributor : MicroserviceHealthContributor {
    override val name: String = "database"

    override suspend fun check(): List<MicroserviceHealthCheckResult> {
        val databases = DatabaseHealthRegistry.snapshot()

        return supervisorScope {
            databases
                .map { database ->
                    async {
                        checkDatabase(database)
                    }
                }
                .awaitAll()
        }
    }

    private suspend fun checkDatabase(
        database: RegisteredDatabase
    ): MicroserviceHealthCheckResult = try {
        val completed = withTimeoutOrNull(DATABASE_HEALTH_TIMEOUT) {
            suspendTransaction(db = database.api.database) {
                exec("SELECT 1")
            }

            true
        } ?: false

        if (completed) {
            healthyResult(database.name)
        } else {
            unhealthyResult(
                instance = database.name,
                message = "Database health check timed out after $DATABASE_HEALTH_TIMEOUT."
            )
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Throwable) {
        unhealthyResult(
            instance = database.name,
            message = exception.message
                ?.takeIf { it.isNotBlank() }
                ?: exception.javaClass.name
        )
    }

    private fun healthyResult(
        instance: String
    ) = MicroserviceHealthCheckResult(
        component = name,
        instance = instance,
        status = MicroserviceHealthStatus.HEALTHY
    )

    private fun unhealthyResult(
        instance: String,
        message: String
    ) = MicroserviceHealthCheckResult(
        component = name,
        instance = instance,
        status = MicroserviceHealthStatus.UNHEALTHY,
        message = message
    )
}