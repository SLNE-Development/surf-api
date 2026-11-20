package dev.slne.surf.database.spring

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.config.SurfConfigApi
import dev.slne.surf.api.spring.SurfProperties
import dev.slne.surf.database.config.DatabaseConfig
import dev.slne.surf.database.config.validate
import dev.slne.surf.database.migration.MigrationSet
import dev.slne.surf.database.migration.SurfMigrationRunner
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.core.env.Environment
import org.springframework.core.env.get
import kotlin.io.path.Path

/**
 * Provides a database as beans, built from `database.yml`.
 *
 * - [DatabaseConfig]: read from `<directory>/database.yml` with the `SURF_DATABASE_*` overrides, unless the application
 *   defines the bean. The directory is the property [SurfProperties.CONFIG_DIRECTORY] and defaults to `config`. Outside
 *   a microservice, the environment variable `SURF_CONFIG_DIRECTORY` sets it; a microservice sets the property from
 *   `SURF_MICROSERVICE_CONFIG_DIR`, so `SURF_CONFIG_DIRECTORY` has no effect there. An application's own bean is
 *   validated and then used verbatim: the `SURF_DATABASE_*` overrides do not apply to it.
 * - [SurfMigrationRunner]: runs every [MigrationSet] bean in `@Order` order while the context starts; a failed
 *   migration fails the start and releases the pool.
 * - [R2dbcDatabase]: created only after the migrations ran, so every bean that injects it sees the migrated schema.
 *   Every [SurfDatabaseConfigurer] bean customizes it.
 *
 * Exposed's `defaultMaxAttempts` is set to 1, so a transaction never runs again on its own: a block with side effects
 * outside the database must not repeat unnoticed. Retries are explicit, through `retryingTransaction`. The setting is
 * applied before the configurers, so a [SurfDatabaseConfigurer] can restore Exposed's default.
 *
 * The connection pool and the JDBC data source of the migrations are not beans of their own, so neither Spring Boot's
 * R2DBC and Flyway auto-configurations nor application code pick them up. In the published jar, R2DBC, Flyway and
 * Exposed are relocated, which keeps Spring Boot's conditions on those libraries from matching as well. When the
 * context closes, the database is unregistered from Exposed and the pool and its event loop are disposed.
 */
@AutoConfiguration
class SurfDatabaseAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun surfDatabaseConfig(
        configApi: SurfConfigApi,
        environment: Environment,
    ): DatabaseConfig {
        val directory = Path(
            environment[SurfProperties.CONFIG_DIRECTORY] ?: SurfProperties.DEFAULT_CONFIG_DIRECTORY
        )

        return DatabaseConfig.load(configApi, directory)
    }

    @Bean(RESOURCES_BEAN, destroyMethod = "close")
    internal fun surfDatabaseResources(
        config: DatabaseConfig,
        configurers: ObjectProvider<SurfDatabaseConfigurer>,
        environment: Environment,
    ): SurfDatabaseResources {
        config.validate()
        val ordered = configurers.orderedStream().toList()
        return SurfDatabaseResources.open(
            config,
            environment[SurfProperties.APPLICATION_NAME] ?: DEFAULT_POOL_NAME
        ) {
            defaultMaxAttempts = NO_IMPLICIT_RETRIES
            ordered.forEach { it.configure(this) }
        }
    }

    @Bean(MIGRATION_RUNNER_BEAN)
    internal fun surfMigrationRunner(
        resources: SurfDatabaseResources,
        sets: ObjectProvider<MigrationSet>,
    ): SurfMigrationRunner {
        val runner = SurfMigrationRunner(resources.database, resources.migrationDataSource)
        val ordered = sets.orderedStream().toList()

        runBlocking {
            for (set in ordered) {
                val result = runner.migrate(set)
                log.atInfo()
                    .log(
                        "Migrated '%s': %d migrations executed, from version %s",
                        set.name,
                        result.migrationsExecuted,
                        result.initialVersion,
                    )
            }
        }

        return runner
    }

    @Bean(DATABASE_BEAN, destroyMethod = "")
    @DependsOn(MIGRATION_RUNNER_BEAN)
    internal fun surfDatabase(resources: SurfDatabaseResources): R2dbcDatabase = resources.database

    private companion object {

        private const val NO_IMPLICIT_RETRIES = 1
        private const val DEFAULT_POOL_NAME = "surf"
        private const val RESOURCES_BEAN = "surfDatabaseResources"
        private const val MIGRATION_RUNNER_BEAN = "surfMigrationRunner"
        private const val DATABASE_BEAN = "surfDatabase"

        private val log = logger()
    }
}
