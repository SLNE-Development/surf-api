package dev.slne.surf.database.testing

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.TestOnlyDatabaseApi
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import javax.sql.DataSource

/**
 * Runs [block] with a [DatabaseApi] whose R2DBC factory points at a closed port, for tests that never connect.
 * The API is shut down afterwards.
 */
@OptIn(TestOnlyDatabaseApi::class)
fun <T> withOfflineDatabaseApi(
    migrationDataSource: DataSource? = null,
    block: suspend (DatabaseApi) -> T
): T = runBlocking {
    val factory = MariadbConnectionFactory.from(
        MariadbConnectionConfiguration.builder().host("localhost").port(1).username("x").build()
    )
    val api = DatabaseApi.create(
        connectionFactory = factory,
        dialect = MariaDBDialect(),
        migrationDataSource = migrationDataSource
    )
    try {
        block(api)
    } finally {
        api.shutdown()
    }
}
