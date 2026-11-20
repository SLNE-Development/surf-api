package dev.slne.surf.database.testing

import dev.slne.surf.database.config.internal.DatabaseConnector
import dev.slne.surf.database.logger.ComponentSqlLogger
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.jetbrains.exposed.v1.core.vendors.DatabaseDialect
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import org.slf4j.event.Level

/**
 * An R2DBC factory pointing at a closed port, for databases that never connect.
 */
fun offlineConnectionFactory(): ConnectionFactory = MariadbConnectionFactory.from(
    MariadbConnectionConfiguration.builder().host("localhost").port(1).username("x").build()
)

/**
 * Runs [block] with an [R2dbcDatabase] connected like the library does, whose R2DBC factory points at a closed port,
 * for tests that never connect. The database is unregistered from Exposed afterwards.
 *
 * @param customize runs last on the database configuration, as a `SurfDatabaseConfigurer` does
 */
fun <T> withOfflineDatabase(
    dialect: DatabaseDialect = MariaDBDialect(),
    customize: R2dbcDatabaseConfig.Builder.() -> Unit = {},
    block: suspend (R2dbcDatabase) -> T,
): T = runBlocking {
    val database = DatabaseConnector.connect(
        offlineConnectionFactory(),
        dialect,
        ComponentSqlLogger(ComponentLogger.logger("OfflineDatabase"), Level.DEBUG),
        customize,
    )
    try {
        block(database)
    } finally {
        TransactionManager.closeAndUnregister(database)
    }
}
