package dev.slne.surf.microservice.database.integration

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.migration.MigrationResult
import dev.slne.surf.database.migration.migrate
import dev.slne.surf.microservice.api.integration.MicroserviceIntegration
import dev.slne.surf.microservice.api.integration.MicroserviceIntegrationContext
import dev.slne.surf.microservice.database.DatabaseCustomizer
import dev.slne.surf.microservice.database.migration.ConventionalMigrations
import dev.slne.surf.microservice.database.migration.migrationName
import dev.slne.surf.microservice.database.nats.PoolSizedHandlerLimits
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

private val log = logger()

/**
 * Opens the database of the microservice, migrates it and closes it on shutdown.
 *
 * The configuration comes from `database.yml` in the data path, the pool is named after the service, and the
 * migration history lives in `flyway_schema_history_<service>` with `-` replaced by `_`. SQL migrations come from
 * `db/migration`, or from `db/migration/<vendor>` (`mariadb`, `postgresql`) once the module has such vendor folders,
 * see [ConventionalMigrations]. A microservice class that implements [DatabaseCustomizer] adjusts these defaults.
 * The database is published through [DatabaseHolder] only after the migrations succeeded. A failed start closes it
 * again.
 *
 * After the migrations, [applyHandlerLimits] sizes the default NATS handler limits to the pool, see
 * [PoolSizedHandlerLimits]. The NATS integration starts later, so the limits apply to every service registration.
 */
@AutoService(MicroserviceIntegration::class)
internal class DatabaseIntegration internal constructor(
    private val openDatabase: (MicroserviceIntegrationContext, DatabaseCustomizer?) -> DatabaseApi,
    private val applyHandlerLimits: (DatabaseApi) -> Unit,
) : MicroserviceIntegration {

    @Suppress("unused") // Used by autoservice
    constructor() : this(::openConfiguredDatabase, { api -> PoolSizedHandlerLimits().apply(api.poolMaxSize) })

    override val name = "database"
    override val order = MicroserviceIntegration.DATABASE_ORDER

    override suspend fun start(context: MicroserviceIntegrationContext) {
        val customizer = context.microservice as? DatabaseCustomizer
        val api = open(context, customizer)

        try {
            migrate(api, context, customizer)
            applyHandlerLimits(api)
        } catch (exception: Throwable) {
            closeAfterFailedStart(api, exception)
            throw exception
        }

        DatabaseHolder.install(api)
    }

    override suspend fun stop(context: MicroserviceIntegrationContext) {
        val api = DatabaseHolder.uninstall() ?: return
        withContext(NonCancellable + Dispatchers.IO) {
            api.shutdown()
        }
    }

    private suspend fun open(
        context: MicroserviceIntegrationContext,
        customizer: DatabaseCustomizer?,
    ) = withContext(Dispatchers.IO) {
        openDatabase(context, customizer)
    }

    private suspend fun migrate(
        api: DatabaseApi,
        context: MicroserviceIntegrationContext,
        customizer: DatabaseCustomizer?,
    ) {
        val classLoader = context.microservice.javaClass.classLoader
        val conventions = ConventionalMigrations.select(customizer, classLoader, api.databaseType)
        if (conventions == null) {
            log.atInfo()
                .log("No database migrations for %s", context.name)
            return
        }

        val result = api.migrate(migrationName(context.name)) {
            conventions.sqlLocation?.let { location -> sql(location) }
            classes(*conventions.classes.toTypedArray())
            classLoader(classLoader)
            customizer?.let { with(it) { configureMigrations() } }
        }

        logResult(context.name, result)
    }

    private fun logResult(serviceName: String, result: MigrationResult) {
        if (result.migrationsExecuted == 0) {
            log.atInfo()
                .log("Database schema of %s is up to date", serviceName)
        } else {
            log.atInfo()
                .log(
                    "Migrated database schema of %s from version %s to %s (%d migrations)",
                    serviceName,
                    result.initialVersion ?: "<empty>",
                    result.targetVersion,
                    result.migrationsExecuted,
                )
        }
    }

    private suspend fun closeAfterFailedStart(api: DatabaseApi, cause: Throwable) {
        try {
            withContext(NonCancellable + Dispatchers.IO) {
                api.shutdown()
            }
        } catch (shutdownFailure: Throwable) {
            cause.addSuppressed(shutdownFailure)
        }
    }
}

private const val CONFIG_FILE_NAME = "database.yml"

private fun openConfiguredDatabase(
    context: MicroserviceIntegrationContext,
    customizer: DatabaseCustomizer?,
) = DatabaseApi.create(
    pluginPath = context.dataPath,
    fileName = CONFIG_FILE_NAME,
    poolName = context.name,
    configCustomizer = { customizer?.let { with(it) { configureDatabase() } } },
)
