package dev.slne.surf.database.spring

import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig

/**
 * Customizes Exposed's configuration of the database that [SurfDatabaseAutoConfiguration] connects.
 *
 * Every bean runs in `@Order` order after the library's defaults are set, including
 * `defaultMaxAttempts = 1`, which a bean can raise to restore Exposed's implicit retries. For
 * example:
 *
 * ```
 * @Bean
 * fun statementLimits() = SurfDatabaseConfigurer { it.defaultFetchSize = 500 }
 * ```
 *
 * Replacing `typeMapping` drops the library's UTC mapping of Exposed's `timestamp()` columns.
 */
fun interface SurfDatabaseConfigurer {

    /**
     * Applies the customization to [builder].
     */
    fun configure(builder: R2dbcDatabaseConfig.Builder)
}
