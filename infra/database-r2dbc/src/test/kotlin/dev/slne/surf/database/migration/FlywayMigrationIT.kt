package dev.slne.surf.database.migration

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.TestOnlyDatabaseApi
import dev.slne.surf.database.migration.fixtures.plugina.V2_JdbcInsert
import dev.slne.surf.database.migration.fixtures.plugina.V3_ExposedInsert
import dev.slne.surf.database.migration.internal.FlywayMigrationRunner
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.toDatabaseApi
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.SQLException
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIT {

    companion object {
        @JvmStatic
        fun databases(): List<Arguments> = listOf(
            Arguments.of("mariadb"),
            Arguments.of("postgres"),
        )

        private fun container(db: String): JdbcDatabaseContainer<*> = when (db) {
            "mariadb" -> DatabaseContainers.mariadb()
            else -> DatabaseContainers.postgres()
        }
    }

    private fun withApi(db: String, block: suspend (DatabaseApi) -> Unit) = runBlocking {
        val api = container(db).toDatabaseApi(fresh = true)
        try {
            block(api)
        } finally {
            api.shutdown()
        }
    }

    private fun DatabaseApi.execute(sql: String) {
        assertNotNull(migrationDataSource).connection.use { c -> c.createStatement().use { it.execute(sql) } }
    }

    private fun DatabaseApi.count(table: String): Int =
        assertNotNull(migrationDataSource).connection.use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT COUNT(*) FROM $table").use { rs -> rs.next(); rs.getInt(1) }
            }
        }

    private fun DatabaseApi.tableExists(table: String): Boolean = try {
        count(table)
        true
    } catch (_: SQLException) {
        false
    }

    /** Rows of a history table as `version:type:success`, ordered by rank; empty if the table does not exist. */
    private fun DatabaseApi.history(table: String): List<String> = try {
        assertNotNull(migrationDataSource).connection.use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT version, type, success FROM $table ORDER BY installed_rank").use { rs ->
                    buildList {
                        while (rs.next()) add("${rs.getString(1)}:${rs.getString(2)}:${rs.getBoolean(3)}")
                    }
                }
            }
        }
    } catch (_: SQLException) {
        emptyList()
    }

    private fun DatabaseApi.script(table: String, version: String): String? =
        assertNotNull(migrationDataSource).connection.use { c ->
            c.prepareStatement("SELECT script FROM $table WHERE version = ?").use { s ->
                s.setString(1, version)
                s.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
            }
        }

    @ParameterizedTest
    @MethodSource("databases")
    fun appliesSqlAndCodeMigrationsInOrder(db: String) = withApi(db) { api ->
        val result = api.migrate("plugin_a") {
            sql("migrations/plugin_a")
            classes(V2_JdbcInsert, V3_ExposedInsert)
        }

        assertEquals(3, result.migrationsExecuted)
        assertEquals("3", result.targetVersion)
        assertNull(result.initialVersion)
        assertEquals(2, api.count("a_items"))
        assertTrue(api.tableExists("flyway_schema_history_plugin_a"))
        assertEquals(V2_JdbcInsert::class.java.name, api.script("flyway_schema_history_plugin_a", "2"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun secondRunIsNoOp(db: String) = withApi(db) { api ->
        val spec: MigrationSpec.() -> Unit = {
            sql("migrations/plugin_a")
            classes(V2_JdbcInsert, V3_ExposedInsert)
        }
        api.migrate("plugin_a", spec)

        val second = api.migrate("plugin_a", spec)

        assertEquals(0, second.migrationsExecuted)
        assertEquals("3", second.initialVersion)
        assertEquals(2, api.count("a_items"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun classPackageScanFindsMigrations(db: String) = withApi(db) { api ->
        val result = api.migrate("scan") {
            classPackage("dev.slne.surf.database.migration.fixtures.scan")
        }

        assertEquals(3, result.migrationsExecuted)
        assertEquals("5", result.targetVersion)
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun twoPluginsWithSameVersionsDoNotConflict(db: String) = withApi(db) { api ->
        val a = api.migrate("plugin_a") { sql("migrations/plugin_a") }
        val b = api.migrate("plugin_b") { sql("migrations/plugin_b") }

        assertEquals(1, a.migrationsExecuted)
        assertEquals(1, b.migrationsExecuted)
        assertTrue(api.tableExists("a_items"))
        assertTrue(api.tableExists("b_items"))
        assertContains(api.history("flyway_schema_history_plugin_a"), "1:SQL:true")
        assertContains(api.history("flyway_schema_history_plugin_b"), "1:SQL:true")
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun newPluginOnSharedDatabaseRunsV1(db: String) = withApi(db) { api ->
        api.execute("CREATE TABLE unrelated (id INT PRIMARY KEY)")

        val result = api.migrate("plugin_a") { sql("migrations/plugin_a") }

        assertEquals(1, result.migrationsExecuted)
        assertTrue(api.tableExists("a_items"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun existingTablesAreBaselinedAndIdempotentV1Passes(db: String) = withApi(db) { api ->
        api.execute("CREATE TABLE a_items (id INT PRIMARY KEY, name VARCHAR(64) NOT NULL)")

        val result = api.migrate("plugin_a") { sql("migrations/plugin_a") }

        assertEquals(1, result.migrationsExecuted)
        assertEquals(
            listOf("0:BASELINE:true", "1:SQL:true"),
            api.history("flyway_schema_history_plugin_a")
        )
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun failingSqlMigrationThrowsAndIsNotRecorded(db: String) = withApi(db) { api ->
        val error = assertFailsWith<IllegalStateException> {
            api.migrate("failing") { sql("migrations/failing") }
        }

        assertContains(assertNotNull(error.message), "1")
        assertContains(assertNotNull(error.message), "failing")
        assertFalse(api.history("flyway_schema_history_failing").any { it.startsWith("1:") && it.endsWith(":true") })
    }

    @OptIn(TestOnlyDatabaseApi::class)
    @Test
    fun migrateWithoutDataSourceFails() = runBlocking {
        val factory = MariadbConnectionFactory.from(
            MariadbConnectionConfiguration.builder().host("localhost").port(1).username("x").build()
        )
        val api = DatabaseApi.create(connectionFactory = factory, dialect = MariaDBDialect())
        try {
            val error = assertFailsWith<IllegalStateException> {
                api.migrate("plugin_a") { sql("migrations/plugin_a") }
            }
            assertContains(assertNotNull(error.message), "migrationDataSource")
        } finally {
            api.shutdown()
        }
    }

    @Test
    fun cleanIsDisabled() = withApi("mariadb") { api ->
        val plan = MigrationSpec(javaClass.classLoader).apply { sql("migrations/plugin_a") }.build("plugin_a")

        val configuration = FlywayMigrationRunner(api).configure(plan)

        assertTrue(configuration.isCleanDisabled)
    }

    @Test
    fun withoutSqlNoDefaultLocationIsUsed() = withApi("mariadb") { api ->
        val plan = MigrationSpec(javaClass.classLoader).apply { classes(V2_JdbcInsert) }.build("plugin_a")

        val configuration = FlywayMigrationRunner(api).configure(plan)

        assertTrue(configuration.locations.isEmpty())
    }
}
