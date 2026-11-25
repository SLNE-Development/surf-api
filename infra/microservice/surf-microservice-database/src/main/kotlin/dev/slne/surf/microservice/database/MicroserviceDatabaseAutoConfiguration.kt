package dev.slne.surf.microservice.database

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.database.config.DatabaseConfig
import dev.slne.surf.database.migration.MigrationSet
import dev.slne.surf.database.spring.SurfDatabaseAutoConfiguration
import dev.slne.surf.microservice.api.MicroserviceInfo
import dev.slne.surf.microservice.database.health.DatabaseHealthIndicator
import dev.slne.surf.microservice.database.migration.ConventionalMigrations
import dev.slne.surf.microservice.database.migration.ServiceMigrationSet
import dev.slne.surf.microservice.database.migration.migrationName
import dev.slne.surf.microservice.database.nats.PoolSizedHandlerLimits
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.Bean

/**
 * Connects a microservice to the database that [SurfDatabaseAutoConfiguration] provides.
 *
 * - The microservice's own [MigrationSet], named after the service. It migrates the SQL files in
 *   `db/migration`, or in `db/migration/<vendor>` (`mariadb`, `postgresql`) once the application
 *   has vendor folders, and the `SurfMigration` classes below the application's auto-configuration
 *   packages. It runs at [MIGRATION_SET_ORDER], before the sets of libraries. It is absent if the
 *   application ships neither SQL nor migration classes, so a service that creates its schema
 *   differently gets no empty history table. Set `surf.database.migrations.enabled` to `false` to
 *   skip it.
 * - A health indicator named `database` that runs `SELECT 1`.
 * - The default NATS handler limits, sized to the connection pool, when surf-nats is on the class
 *   path.
 *
 * The configuration applies only to a microservice, which has a [MicroserviceInfo] bean. Services
 * reach the database by injecting `R2dbcDatabase`.
 */
@AutoConfiguration(after = [SurfDatabaseAutoConfiguration::class])
@ConditionalOnBean(MicroserviceInfo::class)
public class MicroserviceDatabaseAutoConfiguration {

    /**
     * Returns the set of the service, or `null` if the application ships no migrations.
     *
     * Resources and classes are looked up through the class loader of [beanFactory], which is the
     * application's.
     *
     * @throws IllegalStateException if the application has SQL folders for other database vendors
     * but not for the configured one
     */
    @Bean(MIGRATION_SET_BEAN)
    @ConditionalOnProperty(
        prefix = "surf.database.migrations",
        name = ["enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    internal fun microserviceMigrationSet(
        info: MicroserviceInfo,
        config: DatabaseConfig,
        beanFactory: ConfigurableListableBeanFactory,
    ): MigrationSet? {
        val classLoader = beanFactory.beanClassLoader ?: javaClass.classLoader
        val applicationPackages = if (AutoConfigurationPackages.has(beanFactory)) {
            AutoConfigurationPackages.get(beanFactory)
        } else {
            emptyList()
        }

        val migrations = ConventionalMigrations.discover(
            classLoader,
            config.credentials.databaseType,
            applicationPackages,
        )

        if (migrations == null) {
            log.atInfo()
                .log("No database migrations found for %s", info.name)

            return null
        }

        return ServiceMigrationSet(
            migrationName(info.name),
            migrations,
            classLoader,
            MIGRATION_SET_ORDER,
        )
    }

    @Bean(HEALTH_INDICATOR_BEAN)
    internal fun databaseHealthIndicator(database: R2dbcDatabase): HealthIndicator {
        return DatabaseHealthIndicator { suspendTransaction(db = database) { exec("SELECT 1") } }
    }

    /**
     * Applies the pool-sized NATS handler limits while the context starts, before surf-nats
     * registers any service.
     */
    @Bean(HANDLER_LIMITS_BEAN)
    internal fun poolSizedHandlerLimits(config: DatabaseConfig): SmartInitializingSingleton {
        return SmartInitializingSingleton {
            PoolSizedHandlerLimits().apply(config.pool.sizing.maxSize)
        }
    }

    public companion object {

        /**
         * The order of the service's migration set: the default priority, so it runs before the
         * sets of libraries, which use
         * [LOWEST_PRECEDENCE][org.springframework.core.Ordered.LOWEST_PRECEDENCE]. A set that must
         * run before the service's schema exists takes a lower value.
         */
        public const val MIGRATION_SET_ORDER: Int = 0

        private const val MIGRATION_SET_BEAN = "microserviceMigrationSet"
        private const val HEALTH_INDICATOR_BEAN = "database"
        private const val HANDLER_LIMITS_BEAN = "microserviceDatabaseHandlerLimits"

        private val log = logger()
    }
}
