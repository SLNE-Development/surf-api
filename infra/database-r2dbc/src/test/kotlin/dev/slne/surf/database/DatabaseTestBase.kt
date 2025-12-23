package dev.slne.surf.database

import dev.slne.database.DatabaseApi
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.ConnectionFactoryOptions.DATABASE
import io.r2dbc.spi.ConnectionFactoryOptions.DRIVER
import io.r2dbc.spi.ConnectionFactoryOptions.HOST
import io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD
import io.r2dbc.spi.ConnectionFactoryOptions.PORT
import io.r2dbc.spi.ConnectionFactoryOptions.USER
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.properties.Delegates

@Testcontainers
abstract class DatabaseTestBase {

    companion object {
        @JvmStatic
        @Container
        val mariaDb: MariaDBContainer<*> = MariaDBContainer("mariadb")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
    }

    var databaseApi: DatabaseApi by Delegates.notNull()

    @BeforeEach
    fun setupDatabase() {
        val options = ConnectionFactoryOptions.builder()
            .option(DRIVER, "mariadb")
            .option(HOST, mariaDb.host)
            .option(PORT, mariaDb.firstMappedPort)
            .option(USER, mariaDb.username)
            .option(PASSWORD, mariaDb.password)
            .option(DATABASE, mariaDb.databaseName)
            .build()

        val config = ConnectionPoolConfiguration.builder()
            .connectionFactory(ConnectionFactories.get(options))
            .build()

        val pool = ConnectionPool(config)

        databaseApi = DatabaseApi.create(pool)
    }

    @AfterEach
    fun shutdownDatabase() {
        databaseApi.shutdown()
    }
}