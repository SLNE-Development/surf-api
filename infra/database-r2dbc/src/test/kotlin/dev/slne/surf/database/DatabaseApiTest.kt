package dev.slne.surf.database

import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.testing.offlineConnectionFactory
import dev.slne.surf.database.testing.use
import dev.slne.surf.database.testing.withOfflineDatabaseApi
import io.r2dbc.spi.IsolationLevel
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.transactions.transactionManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mariadb.jdbc.MariaDbDataSource
import org.postgresql.ds.PGSimpleDataSource
import org.slf4j.event.Level
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Checks how [DatabaseApi.create] sets up the JDBC data source for migrations. Nothing here connects to a database.
 */
class DatabaseApiTest {

    /** The surf config loader maps properties to kebab-case keys (`database-type`, `initial-size`, ...). */
    private fun createFromConfig(dir: Path, credentials: String): DatabaseApi {
        dir.resolve("database.yml").writeText(
            "credentials:\n" + credentials.trimIndent().prependIndent("  ") + "\n" + """
                pool:
                  sizing:
                    initial-size: 0
                    min-idle: 0
                    max-size: 1
            """.trimIndent()
        )
        return DatabaseApi.create(pluginPath = dir, fileName = "database.yml", poolName = "api-test")
    }

    @Test
    fun configBasedMariaDbApiHasMariaDbMigrationDataSource(@TempDir dir: Path) {
        val api = createFromConfig(
            dir,
            """
            database-type: MARIADB
            host: db.example
            port: 3307
            database: plugins
            username: surf
            password: secret
            """
        )

        api.use {
            val dataSource = assertIs<MariaDbDataSource>(it.migrationDataSource)
            assertContains(dataSource.url, "jdbc:mariadb://db.example:3307/plugins")
            assertEquals("surf", dataSource.user)
        }
    }

    @Test
    fun configBasedPostgresApiHasPostgresMigrationDataSourceWithSchema(@TempDir dir: Path) {
        val api = createFromConfig(
            dir,
            """
            database-type: POSTGRESQL
            schema: plugin_schema
            host: db.example
            port: 5433
            database: plugins
            username: surf
            password: secret
            """
        )

        api.use {
            val dataSource = assertIs<PGSimpleDataSource>(it.migrationDataSource)
            assertContentEquals(arrayOf("db.example"), dataSource.serverNames)
            assertContentEquals(intArrayOf(5433), dataSource.portNumbers)
            assertEquals("plugins", dataSource.databaseName)
            assertEquals("plugin_schema", dataSource.currentSchema)
            assertEquals("surf", dataSource.user)
        }
    }

    @Test
    fun configWithoutPortUsesThePostgresDefaultPort(@TempDir dir: Path) {
        val api = createFromConfig(
            dir,
            """
            database-type: POSTGRESQL
            host: db.example
            """
        )

        api.use {
            assertContentEquals(intArrayOf(5432), assertIs<PGSimpleDataSource>(it.migrationDataSource).portNumbers)
        }
    }

    @Test
    fun configWithoutPortUsesTheMariaDbDefaultPort(@TempDir dir: Path) {
        val api = createFromConfig(
            dir,
            """
            database-type: MARIADB
            host: db.example
            """
        )

        api.use {
            // The driver leaves its default port 3306 out of the URL; any other port would be listed.
            assertContains(assertIs<MariaDbDataSource>(it.migrationDataSource).url, "jdbc:mariadb://db.example/")
        }
    }

    @Test
    fun configBasedApiHasTheConfiguredDatabaseType(@TempDir dir: Path) {
        val api = createFromConfig(
            dir,
            """
            database-type: POSTGRESQL
            host: db.example
            """
        )

        api.use { assertEquals(DatabaseType.POSTGRESQL, it.databaseType) }
    }

    @Test
    fun configBasedApiHasTheConfiguredPoolMaxSize(@TempDir dir: Path) {
        val api = createFromConfig(
            dir,
            """
            database-type: POSTGRESQL
            host: db.example
            """
        )

        api.use { assertEquals(1, it.poolMaxSize) }
    }

    @Test
    fun manualApiHasNoPoolMaxSize() {
        assertNull(withOfflineDatabaseApi { it.poolMaxSize })
    }

    @Test
    fun manualApiDerivesTheDatabaseTypeFromTheDialect() {
        assertEquals(DatabaseType.MARIADB, withOfflineDatabaseApi { it.databaseType })
        assertEquals(
            DatabaseType.POSTGRESQL,
            withOfflineDatabaseApi(dialect = PostgreSQLDialect()) { it.databaseType }
        )
    }

    @OptIn(TestOnlyDatabaseApi::class)
    @Test
    fun manualApiAcceptsADialectWithoutDatabaseType() {
        val api = DatabaseApi.create(offlineConnectionFactory(), H2Dialect())

        api.use { assertSame(api.database, it.database) }
    }

    @Test
    fun databaseTypeOfADialectWithoutDatabaseTypeFails() {
        val failure = withOfflineDatabaseApi(dialect = H2Dialect()) { api ->
            assertFailsWith<IllegalStateException> { api.databaseType }
        }

        assertContains(failure.message.orEmpty(), "H2")
    }

    @Test
    fun vendorIsTheLowercaseTypeName() {
        assertEquals(listOf("mariadb", "postgresql"), DatabaseType.entries.map { it.vendor })
    }

    @OptIn(TestOnlyDatabaseApi::class)
    @Test
    fun manualApiAcceptsTheConfigCustomizerAsTrailingLambda() {
        var customized = false

        DatabaseApi.create(offlineConnectionFactory(), MariaDBDialect()) { customized = true }.shutdown()

        assertTrue(customized)
    }

    @OptIn(TestOnlyDatabaseApi::class)
    @Test
    fun manualApiAcceptsPositionalArgumentsWithoutDataSource() {
        var customized = false

        val api = DatabaseApi.create(
            offlineConnectionFactory(),
            MariaDBDialect(),
            ComponentLogger.logger("positional"),
            Level.INFO,
            { customized = true }
        )

        api.use { assertNull(it.migrationDataSource) }
        assertTrue(customized)
    }

    @Test
    fun manualApiHasNoMigrationDataSourceByDefault() = withOfflineDatabaseApi { api ->
        assertNull(api.migrationDataSource)
    }

    @Test
    fun manualApiKeepsTheGivenMigrationDataSource() {
        val dataSource = PGSimpleDataSource()

        withOfflineDatabaseApi(dataSource) { api -> assertSame(dataSource, api.migrationDataSource) }
    }

    @Test
    fun transactionsDefaultToReadCommitted() = withOfflineDatabaseApi { api ->
        assertEquals(IsolationLevel.READ_COMMITTED, api.database.transactionManager.defaultIsolationLevel)
    }

    @OptIn(TestOnlyDatabaseApi::class)
    @Test
    fun configCustomizerCanOverrideTheIsolationLevel() {
        val api = DatabaseApi.create(offlineConnectionFactory(), MariaDBDialect()) {
            defaultR2dbcIsolationLevel = IsolationLevel.SERIALIZABLE
        }

        api.use {
            assertEquals(IsolationLevel.SERIALIZABLE, it.database.transactionManager.defaultIsolationLevel)
        }
    }
}
