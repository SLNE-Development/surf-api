package dev.slne.surf.database

import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.core.Table
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.javatime.datetime
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.insert
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.selectAll
import dev.slne.surf.database.libs.org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import dev.slne.surf.database.migration.migrate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Runs plugin migrations from the shaded jar.
 *
 * This source set only sees the shaded jar (plus test libraries and the surf runtime), so Flyway, the JDBC drivers,
 * Exposed and R2DBC are only available under their relocated names. The SQL file is not on the test classpath; it is
 * copied into a temp directory and served by an isolated class loader that cannot see the shaded jar, like a plugin
 * class loader would be.
 *
 * MariaDB runs as a plain container: `MariaDBContainer` needs the unshaded JDBC driver to wait for startup, and that
 * driver must not be on this classpath.
 */
@Testcontainers(disabledWithoutDocker = true)
class ShadedFlywayTest {

    companion object {
        private const val POSTGRES_SCHEMA = "smoke_schema"

        private const val MARIADB_DATABASE = "smoke"
        private const val MARIADB_USER = "smoke"
        private const val MARIADB_PASSWORD = "smoke"

        private val started = mutableListOf<GenericContainer<*>>()

        private val mariadb by lazy {
            GenericContainer("mariadb:11")
                .withExposedPorts(3306)
                .withEnv("MARIADB_RANDOM_ROOT_PASSWORD", "yes")
                .withEnv("MARIADB_DATABASE", MARIADB_DATABASE)
                .withEnv("MARIADB_USER", MARIADB_USER)
                .withEnv("MARIADB_PASSWORD", MARIADB_PASSWORD)
                // The entrypoint starts a temporary server for initialisation first, then the real one.
                .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2))
                .also { it.start(); started += it }
        }
        private val postgres by lazy {
            PostgreSQLContainer("postgres:17").also {
                it.start()
                started += it
                // A non-default schema proves the relocated runner still reads PGSimpleDataSource.currentSchema.
                it.psql("CREATE SCHEMA IF NOT EXISTS $POSTGRES_SCHEMA")
            }
        }

        @JvmStatic
        @AfterAll
        fun stopContainers() {
            started.forEach { it.stop() }
            started.clear()
        }

        private fun PostgreSQLContainer.psql(sql: String): String {
            val result = execInContainer("psql", "-U", username, "-d", databaseName, "-tAc", sql)
            check(result.exitCode == 0) { "psql failed: ${result.stderr}" }
            return result.stdout.trim()
        }

        /** Directory holding `db/smoke/V1__smoke.sql`; deliberately not on the test classpath. */
        private val pluginSqlRoot: Path = Paths.get(
            System.getProperty("surf.shadowTest.pluginSql") ?: error("surf.shadowTest.pluginSql is not set")
        )
    }

    @Test
    fun flywayIsOnlyAvailableRelocated() {
        assertFailsWith<ClassNotFoundException> { Class.forName("org.flywaydb.core.Flyway") }
        Class.forName("dev.slne.surf.database.libs.org.flywaydb.core.Flyway")

        val location = DatabaseApi::class.java.protectionDomain.codeSource.location.toString()
        assertTrue(location.endsWith("-all.jar"), "DatabaseApi must come from the shaded jar, was $location")
    }

    @Test
    fun jdbcDriversAreOnlyAvailableRelocated() {
        assertFailsWith<ClassNotFoundException> { Class.forName("org.postgresql.Driver") }
        assertFailsWith<ClassNotFoundException> { Class.forName("org.mariadb.jdbc.Driver") }
        Class.forName("dev.slne.surf.database.libs.org.postgresql.Driver")
        Class.forName("dev.slne.surf.database.libs.org.mariadb.jdbc.Driver")
    }

    private object RoundTripTable : Table("shaded_round_trip") {
        val createdAt = datetime("created_at")
    }

    /**
     * Writes and reads back a datetime through Exposed over R2DBC from the shaded jar. Binding the parameter and
     * reading the column go through Exposed's `TypeMapper`s (`DateTimeTypeMapper`, `DateTimeMySqlTypeMapper`, ...),
     * which Exposed loads with `ServiceLoader` from the merged, relocated `META-INF/services` file, and through the
     * relocated R2DBC driver selected by its `ConnectionFactoryProvider` service entry.
     */
    @ParameterizedTest
    @ValueSource(strings = ["mariadb", "postgres"])
    fun shadedR2dbcRoundTripsDatetime(db: String, @TempDir tempDir: Path) {
        val configDir = tempDir.resolve("config").createDirectories()
        configDir.resolve("database.yml").writeText(databaseYml(db))
        val written = LocalDateTime.of(2026, 10, 21, 13, 37, 42, 123_456_000)

        val api = DatabaseApi.create(pluginPath = configDir, fileName = "database.yml", poolName = "round-trip-$db")
        try {
            val read = runBlocking {
                suspendTransaction(db = api.database) {
                    SchemaUtils.create(RoundTripTable)
                    RoundTripTable.insert { it[createdAt] = written }
                    RoundTripTable.selectAll().map { it[RoundTripTable.createdAt] }.toList().also {
                        // The Postgres schema is shared with the migration test, which checks the exact table list.
                        SchemaUtils.drop(RoundTripTable)
                    }
                }
            }
            assertEquals(listOf(written), read)
        } finally {
            api.shutdown()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["mariadb", "postgres"])
    fun shadedFlywayFindsDatabasePluginsAndPluginResources(db: String, @TempDir tempDir: Path) {
        val pluginDir = tempDir.resolve("plugin").createDirectories()
        pluginSqlRoot.toFile().copyRecursively(pluginDir.toFile())

        URLClassLoader(arrayOf(pluginDir.toUri().toURL()), ClassLoader.getPlatformClassLoader()).use { isolated ->
            assertFailsWith<ClassNotFoundException> { isolated.loadClass(DatabaseApi::class.java.name) }
            check(isolated.getResource("db/smoke/V1__smoke.sql") != null)

            val configDir = tempDir.resolve("config").createDirectories()
            configDir.resolve("database.yml").writeText(databaseYml(db))

            val api = DatabaseApi.create(pluginPath = configDir, fileName = "database.yml", poolName = "shadow-$db")
            try {
                val result = runBlocking {
                    api.migrate("smoke") {
                        sql("db/smoke")
                        classLoader(isolated)
                    }
                }
                assertEquals(1, result.migrationsExecuted)
            } finally {
                api.shutdown()
            }
        }

        if (db == "postgres") {
            val tables = postgres.psql(
                "SELECT string_agg(table_name, ',' ORDER BY table_name) FROM information_schema.tables " +
                    "WHERE table_schema = '$POSTGRES_SCHEMA'"
            )
            assertEquals("flyway_schema_history_smoke,smoke_items", tables)
        }
    }

    /** The surf config loader maps properties to kebab-case keys (`database-type`, `log-level`, ...). */
    private fun databaseYml(db: String): String {
        val credentials = if (db == "mariadb") {
            """
              database-type: MARIADB
              host: ${mariadb.host}
              port: ${mariadb.getMappedPort(3306)}
              database: $MARIADB_DATABASE
              username: $MARIADB_USER
              password: $MARIADB_PASSWORD
            """
        } else {
            """
              database-type: POSTGRESQL
              schema: $POSTGRES_SCHEMA
              host: ${postgres.host}
              port: ${postgres.getMappedPort(5432)}
              database: ${postgres.databaseName}
              username: ${postgres.username}
              password: ${postgres.password}
            """
        }
        return "credentials:\n" + credentials.trimIndent().prependIndent("  ") + "\n" + """
            pool:
              sizing:
                initial-size: 1
                min-idle: 0
                max-size: 2
            log-level: INFO
        """.trimIndent()
    }
}
