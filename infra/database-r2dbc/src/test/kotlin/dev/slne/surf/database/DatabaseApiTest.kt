package dev.slne.surf.database

import dev.slne.surf.database.testing.offlineConnectionFactory
import dev.slne.surf.database.testing.use
import dev.slne.surf.database.testing.withOfflineDatabaseApi
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
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
}
