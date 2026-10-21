package dev.slne.surf.database.migration

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.TestOnlyDatabaseApi
import dev.slne.surf.database.migration.fixtures.plugina.V2_JdbcInsert
import dev.slne.surf.database.migration.fixtures.plugina.V3_ExposedInsert
import dev.slne.surf.database.migration.fixtures.scan.V2_Object
import dev.slne.surf.database.migration.internal.FlywayMigrationRunner
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.toDatabaseApi
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mariadb.r2dbc.MariadbConnectionConfiguration
import org.mariadb.r2dbc.MariadbConnectionFactory
import org.testcontainers.containers.JdbcDatabaseContainer
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URLClassLoader
import java.nio.file.Path
import java.sql.Connection
import java.sql.SQLException
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
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

    /**
     * Rows of a history table as `version:type:success`, ordered by rank. Throws if the table does not exist, so an
     * assertion on the history can never pass just because the table is missing.
     */
    private fun DatabaseApi.history(table: String): List<String> =
        assertNotNull(migrationDataSource).connection.use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT version, type, success FROM $table ORDER BY installed_rank").use { rs ->
                    buildList {
                        while (rs.next()) add("${rs.getString(1)}:${rs.getString(2)}:${rs.getBoolean(3)}")
                    }
                }
            }
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
        assertNull(second.targetVersion)
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
        assertEquals("0", result.initialVersion)
        assertEquals("1", result.targetVersion)
        assertEquals(
            listOf("0:BASELINE:true", "1:SQL:true"),
            api.history("flyway_schema_history_plugin_a")
        )
    }

    /**
     * Postgres runs the failing SQL migration in a transaction and rolls it back, so no row is left. MariaDB DDL is
     * not transactional, so Flyway records every failed migration there with `success = false`.
     */
    @ParameterizedTest
    @MethodSource("databases")
    fun failingSqlMigrationIsRecordedOnlyWhereDdlIsNonTransactional(db: String) = withApi(db) { api ->
        val error = assertFailsWith<IllegalStateException> {
            api.migrate("failing") { sql("migrations/failing") }
        }

        val message = assertNotNull(error.message)
        assertContains(message, "Migration 'failing' failed at version 1 (broken)")
        assertContains(message, "V1__broken.sql")
        val expected = if (db == "mariadb") listOf("1:SQL:false") else emptyList()
        assertEquals(expected, api.history("flyway_schema_history_failing"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun failingJdbcMigrationIsRecordedOnlyWhereDdlIsNonTransactional(db: String) = withApi(db) { api ->
        val failing = object : JdbcMigration("1", "broken jdbc") {
            override fun migrate(connection: Connection) {
                connection.createStatement().use { it.execute("THIS IS NOT VALID SQL") }
            }
        }

        val error = assertFailsWith<IllegalStateException> { api.migrate("failing_jdbc") { classes(failing) } }

        assertContains(assertNotNull(error.message), "Migration 'failing_jdbc' failed at version 1 (broken jdbc)")
        val expected = if (db == "mariadb") listOf("1:JDBC:false") else emptyList()
        assertEquals(expected, api.history("flyway_schema_history_failing_jdbc"))
    }

    /**
     * An [ExposedMigration] is non-transactional for Flyway, so Flyway writes a failed row on both databases. On
     * PostgreSQL that insert runs inside the transaction of Flyway's transactional advisory lock and is rolled back
     * with it, so only MariaDB keeps the row.
     */
    @ParameterizedTest
    @MethodSource("databases")
    fun failingExposedMigrationIsRecordedOnlyOnMariaDb(db: String) = withApi(db) { api ->
        val failing = object : ExposedMigration("1", "broken exposed") {
            override suspend fun R2dbcTransaction.migrate() {
                exec("THIS IS NOT VALID SQL")
            }
        }

        val error = assertFailsWith<IllegalStateException> { api.migrate("failing_exposed") { classes(failing) } }

        assertContains(assertNotNull(error.message), "Migration 'failing_exposed' failed at version 1 (broken exposed)")
        val expected = if (db == "mariadb") listOf("1:JDBC:false") else emptyList()
        assertEquals(expected, api.history("flyway_schema_history_failing_exposed"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun missingSqlLocationFails(db: String) = withApi(db) { api ->
        val error = assertFailsWith<IllegalStateException> {
            api.migrate("missing") { sql("migrations/does_not_exist") }
        }

        assertContains(assertNotNull(error.message), "migrations/does_not_exist")
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun misnamedSqlFileFails(db: String) = withApi(db) { api ->
        val error = assertFailsWith<IllegalStateException> {
            api.migrate("misnamed") { sql("migrations/misnamed") }
        }

        assertContains(assertNotNull(error.message), "V1_init.sql")
        assertFalse(api.tableExists("misnamed_items"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun customBaselineSkipsMigrationsUpToIt(db: String) = withApi(db) { api ->
        api.execute("CREATE TABLE a_items (id INT PRIMARY KEY, name VARCHAR(64) NOT NULL)")

        val result = api.migrate("plugin_a") {
            sql("migrations/plugin_a")
            classes(V2_JdbcInsert, V3_ExposedInsert)
            baselineVersion("2")
        }

        assertEquals(1, result.migrationsExecuted)
        assertEquals("2", result.initialVersion)
        assertEquals("3", result.targetVersion)
        assertEquals(1, api.count("a_items"))
        assertEquals(listOf("2:BASELINE:true", "3:JDBC:true"), api.history("flyway_schema_history_plugin_a"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun laterMigrationsRunOnTopOfEarlierRuns(db: String) = withApi(db) { api ->
        api.migrate("plugin_a") { sql("migrations/plugin_a") }

        val second = api.migrate("plugin_a") {
            sql("migrations/plugin_a")
            classes(V2_JdbcInsert, V3_ExposedInsert)
        }

        assertEquals(2, second.migrationsExecuted)
        assertEquals("1", second.initialVersion)
        assertEquals("3", second.targetVersion)
        assertEquals(listOf("1:SQL:true", "2:JDBC:true", "3:JDBC:true"), api.history("flyway_schema_history_plugin_a"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun changedChecksumOfAppliedMigrationFails(db: String) = withApi(db) { api ->
        val original = object : JdbcMigration("1", "checked") {
            override val checksum: Int = 1
            override fun migrate(connection: Connection) = Unit
        }
        val changed = object : JdbcMigration("1", "checked") {
            override val checksum: Int = 2
            override fun migrate(connection: Connection) = Unit
        }
        api.migrate("checked") { classes(original) }

        val error = assertFailsWith<IllegalStateException> { api.migrate("checked") { classes(changed) } }

        assertContains(assertNotNull(error.message), "checksum", ignoreCase = true)
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun migrationGivenTwiceRunsOnce(db: String) = withApi(db) { api ->
        val result = api.migrate("scan") {
            classes(V2_Object)
            classPackage("dev.slne.surf.database.migration.fixtures.scan")
        }

        assertEquals(3, result.migrationsExecuted)
        assertEquals(listOf("2:JDBC:true", "3:JDBC:true", "5:JDBC:true"), api.history("flyway_schema_history_scan"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun sqlIsFoundThroughTheConfiguredClassLoader(db: String, @TempDir dir: Path) = withApi(db) { api ->
        dir.resolve("isolated").createDirectories()
            .resolve("V1__isolated.sql")
            .writeText("CREATE TABLE IF NOT EXISTS isolated_items (id INT PRIMARY KEY);")

        val result = URLClassLoader(arrayOf(dir.toUri().toURL()), null).use { isolated ->
            api.migrate("isolated") {
                sql("isolated")
                classLoader(isolated)
            }
        }

        assertEquals(1, result.migrationsExecuted)
        assertTrue(api.tableExists("isolated_items"))
    }

    @Test
    fun postgresHistoryTableIsCreatedInTheApiSchema() = withApi("postgres") { api ->
        api.migrate("plugin_a") { sql("migrations/plugin_a") }

        val schemas = assertNotNull(api.migrationDataSource).connection.use { c ->
            c.prepareStatement("SELECT table_schema FROM information_schema.tables WHERE table_name = ?").use { s ->
                s.setString(1, "flyway_schema_history_plugin_a")
                s.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.getString(1)) } }
            }
        }

        // Other tests migrate plugin_a in their own schemas of the shared container, so only check the two that matter.
        assertContains(schemas, assertIs<PGSimpleDataSource>(api.migrationDataSource).currentSchema)
        assertFalse("public" in schemas, schemas.toString())
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
