package dev.slne.surf.database.spring

import dev.slne.surf.api.config.surfConfigApi
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseConfig
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.config.databaseType
import dev.slne.surf.database.config.PoolConfig
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mariadb.jdbc.MariaDbDataSource
import org.postgresql.ds.PGSimpleDataSource
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

/**
 * Checks how [SurfDatabaseResources] sets up the JDBC data source for migrations from a `database.yml`. The pool
 * starts empty, so nothing here connects to a database.
 */
@OptIn(InternalSurfApi::class)
class SurfDatabaseResourcesTest {

    /**
     * Loads a `database.yml` with [credentials]. The surf config loader maps properties to kebab-case keys
     * (`database-type`, `initial-size`, ...).
     */
    private fun load(dir: Path, credentials: String): DatabaseConfig {
        dir.resolve(DatabaseConfig.FILE_NAME).writeText(
            "credentials:\n" + credentials.trimIndent().prependIndent("  ") + "\n" + """
                pool:
                  sizing:
                    initial-size: 0
                    min-idle: 0
                    max-size: 1
            """.trimIndent()
        )
        return DatabaseConfig.load(surfConfigApi, dir)
    }

    private fun <T> withResources(dir: Path, credentials: String, block: (SurfDatabaseResources) -> T): T =
        SurfDatabaseResources.open(load(dir, credentials), "resources-test") {}.use(block)

    @Test
    fun mariaDbConfigHasMariaDbMigrationDataSource(@TempDir dir: Path) {
        withResources(
            dir,
            """
            database-type: MARIADB
            host: db.example
            port: 3307
            database: plugins
            username: surf
            password: secret
            """
        ) { resources ->
            val dataSource = assertIs<MariaDbDataSource>(resources.migrationDataSource)
            assertContains(dataSource.url, "jdbc:mariadb://db.example:3307/plugins")
            assertEquals("surf", dataSource.user)
        }
    }

    @Test
    fun postgresConfigHasPostgresMigrationDataSourceWithSchema(@TempDir dir: Path) {
        withResources(
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
        ) { resources ->
            val dataSource = assertIs<PGSimpleDataSource>(resources.migrationDataSource)
            assertContentEquals(arrayOf("db.example"), dataSource.serverNames)
            assertContentEquals(intArrayOf(5433), dataSource.portNumbers)
            assertEquals("plugins", dataSource.databaseName)
            assertEquals("plugin_schema", dataSource.currentSchema)
            assertEquals("surf", dataSource.user)
        }
    }

    @Test
    fun configWithoutPortUsesThePostgresDefaultPort(@TempDir dir: Path) {
        withResources(
            dir,
            """
            database-type: POSTGRESQL
            host: db.example
            """
        ) { resources ->
            assertContentEquals(intArrayOf(5432), assertIs<PGSimpleDataSource>(resources.migrationDataSource).portNumbers)
        }
    }

    @Test
    fun configWithoutPortUsesTheMariaDbDefaultPort(@TempDir dir: Path) {
        withResources(
            dir,
            """
            database-type: MARIADB
            host: db.example
            """
        ) { resources ->
            // The driver leaves its default port 3306 out of the URL; any other port would be listed.
            assertContains(assertIs<MariaDbDataSource>(resources.migrationDataSource).url, "jdbc:mariadb://db.example/")
        }
    }

    @Test
    fun databaseHasTheConfiguredDatabaseType(@TempDir dir: Path) {
        withResources(
            dir,
            """
            database-type: POSTGRESQL
            host: db.example
            """
        ) { resources ->
            assertEquals(DatabaseType.POSTGRESQL, resources.database.databaseType)
        }
    }

    @Test
    fun failureToCreateTheMigrationDataSourceLeavesNoDatabaseRegistered() {
        val config = DatabaseConfig(
            credentials = ConnectionDetails(databaseType = DatabaseType.POSTGRESQL, host = "db.example"),
            pool = PoolConfig(sizing = PoolConfig.Sizing(0, 0, 1)),
        )
        val primaryBefore = TransactionManager.primaryDatabase
        val defaultBefore = TransactionManager.defaultDatabase

        val failure = assertFailsWith<IllegalStateException> {
            SurfDatabaseResources.open(config, "resources-test", { error("no data source") }) {}
        }

        assertEquals("no data source", failure.message)
        assertSame(primaryBefore, TransactionManager.primaryDatabase)
        assertSame(defaultBefore, TransactionManager.defaultDatabase)
    }

    @Test
    fun loadedConfigHasTheConfiguredPoolMaxSize(@TempDir dir: Path) {
        val config = load(
            dir,
            """
            database-type: POSTGRESQL
            host: db.example
            """
        )

        assertEquals(1, config.pool.sizing.maxSize)
    }
}
