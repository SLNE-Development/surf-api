package dev.slne.surf.microservice.reliability.integration

import com.google.auto.service.AutoService
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.reliability.migrateReliabilitySchema
import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.api.integration.MicroserviceIntegration
import dev.slne.surf.microservice.api.integration.MicroserviceIntegrationContext
import dev.slne.surf.microservice.database.databaseApi
import dev.slne.surf.microservice.reliability.ReliabilityConfig
import dev.slne.surf.microservice.reliability.ReliabilityCustomizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val log = logger()

/**
 * Provides idempotent persistent handlers whose events are published after the commit, owned by the microservice's
 * name.
 *
 * Start, after the database and NATS: migrates the library schema with [migrateReliabilitySchema] (also when the
 * service sets `runMigrations = false`, in its own history table), then starts the cleanup of expired stored replies.
 * Stop, after `onDisable`: stops the cleanup.
 */
@AutoService(MicroserviceIntegration::class)
internal class ReliabilityIntegration internal constructor(
    private val databaseApiOf: (Microservice) -> DatabaseApi,
) : MicroserviceIntegration {

    @Suppress("unused") // Used by autoservice
    constructor() : this(Microservice::databaseApi)

    override val name = "reliability"
    override val order = MicroserviceIntegration.RELIABILITY_ORDER

    override suspend fun start(context: MicroserviceIntegrationContext) {
        val config = (context.microservice as? ReliabilityCustomizer)?.reliabilityConfig ?: ReliabilityConfig()
        val databaseApi = requireDatabase(context.microservice)

        migrate(databaseApi, context.name)
        ReliabilityHolder.install(ReliabilityRuntime.start(context.name, databaseApi, config))
    }

    override suspend fun stop(context: MicroserviceIntegrationContext) {
        ReliabilityHolder.uninstall()?.stop()
    }

    private fun requireDatabase(microservice: Microservice): DatabaseApi = try {
        databaseApiOf(microservice)
    } catch (e: IllegalStateException) {
        throw IllegalStateException("surf-microservice-reliability requires the database integration", e)
    }

    private suspend fun migrate(databaseApi: DatabaseApi, serviceName: String) {
        val result = withContext(Dispatchers.IO) { databaseApi.migrateReliabilitySchema() }
        if (result.migrationsExecuted > 0) {
            log.atInfo()
                .log("Migrated the reliability schema for %s to version %s", serviceName, result.targetVersion)
        }
    }
}
