package dev.slne.surf.database.config.internal

import dev.slne.surf.database.columns.time.internal.UtcSessionTypeMapper
import dev.slne.surf.database.config.DatabaseType
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.IsolationLevel
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.vendors.DatabaseDialect
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.mappers.R2dbcRegistryTypeMapping
import org.jetbrains.exposed.v1.r2dbc.mappers.R2dbcTypeMapping

/**
 * Connects Exposed to a [ConnectionFactory] with the library's defaults.
 *
 * Transactions default to READ COMMITTED, so they never read uncommitted rows of other transactions; [customize] may
 * set another `defaultR2dbcIsolationLevel`. Exposed applies the level per transaction, so MariaDB's session variable
 * `@@tx_isolation` does not show it.
 *
 * The type mapping registers [UtcSessionTypeMapper] on top of Exposed's defaults. A [customize] that replaces
 * `typeMapping` drops it, and Exposed's `timestamp()` columns then convert through the JVM default zone again.
 */
internal object DatabaseConnector {

    fun connect(
        connectionFactory: ConnectionFactory,
        dialect: DatabaseDialect,
        sqlLogger: SqlLogger,
        customize: R2dbcDatabaseConfig.Builder.() -> Unit = {},
    ): R2dbcDatabase = R2dbcDatabase.connect(
        connectionFactory,
        R2dbcDatabaseConfig {
            explicitDialect = dialect
            typeMapping = utcTypeMapping()
            this.sqlLogger = sqlLogger
            defaultR2dbcIsolationLevel = IsolationLevel.READ_COMMITTED
            customize()
        },
    )

    private fun utcTypeMapping(): R2dbcTypeMapping {
        val mapping = R2dbcRegistryTypeMapping.default() as R2dbcRegistryTypeMapping
        mapping.register(UtcSessionTypeMapper)
        return mapping
    }
}

/**
 * A new instance of the Exposed dialect of this vendor.
 */
internal fun DatabaseType.newDialect(): DatabaseDialect = when (this) {
    DatabaseType.MARIADB -> MariaDBDialect()
    DatabaseType.POSTGRESQL -> PostgreSQLDialect()
}
