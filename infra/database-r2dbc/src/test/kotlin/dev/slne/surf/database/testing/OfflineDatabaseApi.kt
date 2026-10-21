package dev.slne.surf.database.testing

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.TestOnlyDatabaseApi
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import javax.sql.DataSource

/**
 * Runs [block] with this API and shuts it down afterwards.
 */
inline fun <T> DatabaseApi.use(block: (DatabaseApi) -> T): T = try {
    block(this)
} finally {
    shutdown()
}

/**
 * An R2DBC factory pointing at a closed port, for APIs that never connect.
 */
fun offlineConnectionFactory(): ConnectionFactory = MariadbConnectionFactory.from(
    MariadbConnectionConfiguration.builder().host("localhost").port(1).username("x").build()
)

/**
 * Runs [block] with a [DatabaseApi] whose R2DBC factory points at a closed port, for tests that never connect.
 * The API is shut down afterwards.
 */
@OptIn(TestOnlyDatabaseApi::class)
fun <T> withOfflineDatabaseApi(
    migrationDataSource: DataSource? = null,
    block: suspend (DatabaseApi) -> T
): T = runBlocking {
    DatabaseApi.create(
        connectionFactory = offlineConnectionFactory(),
        dialect = MariaDBDialect(),
        migrationDataSource = migrationDataSource
    ).use { block(it) }
}
