package dev.slne.surf.database.migration

import dev.slne.surf.database.config.databaseType
import dev.slne.surf.database.migration.fixtures.plugina.V2_JdbcInsert
import dev.slne.surf.database.migration.fixtures.plugina.V3_ExposedInsert
import dev.slne.surf.database.migration.fixtures.scan.V2_Object
import dev.slne.surf.database.migration.fixtures.scan.V3_Class
import dev.slne.surf.database.migration.fixtures.scan.nested.V5_Nested
import dev.slne.surf.database.testing.DatabaseContainers
import dev.slne.surf.database.testing.TestDatabase
import dev.slne.surf.database.testing.toTestDatabase
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.net.URLClassLoader
import java.nio.file.Path
import java.sql.Connection
import java.sql.SQLException
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.*

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationIT {

    companion object {
        @JvmStatic
        fun databases(): List<Arguments> {
            return listOf(
                Arguments.of("mariadb"),
                Arguments.of("postgres"),
            )
        }

        private fun container(db: String): JdbcDatabaseContainer<*> = when (db) {
            "mariadb" -> DatabaseContainers.mariadb()
            else -> DatabaseContainers.postgres()
        }
    }

    private fun withTestDatabase(db: String, block: suspend (TestDatabase) -> Unit) = runBlocking {
        val testDb = container(db).toTestDatabase(fresh = true)

        try {
            block(testDb)
        } finally {
            testDb.close()
        }
    }

    private fun TestDatabase.execute(sql: String) {
        migrationDataSource.connection.use { c -> c.createStatement().use { it.execute(sql) } }
    }

    private fun TestDatabase.count(table: String): Int {
        return migrationDataSource.connection.use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT COUNT(*) FROM $table").use { rs -> rs.next(); rs.getInt(1) }
            }
        }
    }

    private fun TestDatabase.tableExists(table: String): Boolean = try {
        count(table)
        true
    } catch (_: SQLException) {
        false
    }

    /**
     * Rows of a history table as `version:type:success`, ordered by rank. Throws if the table does
     * not exist, so an assertion on the history can never pass just because the table is missing.
     */
    private fun TestDatabase.history(table: String): List<String> {
        return migrationDataSource.connection.use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT version, type, success FROM $table ORDER BY installed_rank")
                    .use { rs ->
                        buildList {
                            while (rs.next()) add(
                                "${rs.getString(1)}:${rs.getString(2)}:${
                                    rs.getBoolean(
                                        3,
                                    )
                                }",
                            )
                        }
                    }
            }
        }
    }

    private fun TestDatabase.vendors(table: String): List<String> {
        return migrationDataSource.connection.use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT vendor FROM $table ORDER BY id").use { rs ->
                    buildList { while (rs.next()) add(rs.getString(1)) }
                }
            }
        }
    }

    private fun TestDatabase.script(table: String, version: String): String? {
        return migrationDataSource.connection.use { c ->
            c.prepareStatement("SELECT script FROM $table WHERE version = ?").use { s ->
                s.setString(1, version)
                s.executeQuery().use { rs -> if (rs.next()) rs.getString(1) else null }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun appliesSqlAndCodeMigrationsInOrder(db: String) = withTestDatabase(db) { testDb ->
        val result = testDb.migrate("plugin_a") {
            sql("migrations/plugin_a")
            classes(V2_JdbcInsert, V3_ExposedInsert)
        }

        assertEquals(3, result.migrationsExecuted)
        assertEquals("3", result.targetVersion)
        assertNull(result.initialVersion)
        assertEquals(2, testDb.count("a_items"))
        assertTrue(testDb.tableExists("flyway_schema_history_plugin_a"))
        assertEquals(
            V2_JdbcInsert::class.java.name,
            testDb.script("flyway_schema_history_plugin_a", "2"),
        )
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun secondRunIsNoOp(db: String) = withTestDatabase(db) { testDb ->
        val spec: MigrationSpec.() -> Unit = {
            sql("migrations/plugin_a")
            classes(V2_JdbcInsert, V3_ExposedInsert)
        }
        testDb.migrate("plugin_a", spec)

        val second = testDb.migrate("plugin_a", spec)

        assertEquals(0, second.migrationsExecuted)
        assertEquals("3", second.initialVersion)
        assertNull(second.targetVersion)
        assertEquals(2, testDb.count("a_items"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun classNamesRunMigrations(db: String) = withTestDatabase(db) { testDb ->
        val result = testDb.migrate("named") {
            classNames(
                V2_Object::class.java.name,
                V3_Class::class.java.name,
                V5_Nested::class.java.name,
            )
        }

        assertEquals(3, result.migrationsExecuted)
        assertEquals("5", result.targetVersion)
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun twoPluginsWithSameVersionsDoNotConflict(db: String) = withTestDatabase(db) { testDb ->
        val a = testDb.migrate("plugin_a") { sql("migrations/plugin_a") }
        val b = testDb.migrate("plugin_b") { sql("migrations/plugin_b") }

        assertEquals(1, a.migrationsExecuted)
        assertEquals(1, b.migrationsExecuted)
        assertTrue(testDb.tableExists("a_items"))
        assertTrue(testDb.tableExists("b_items"))
        assertContains(testDb.history("flyway_schema_history_plugin_a"), "1:SQL:true")
        assertContains(testDb.history("flyway_schema_history_plugin_b"), "1:SQL:true")
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun newPluginOnSharedDatabaseRunsV1(db: String) = withTestDatabase(db) { testDb ->
        testDb.execute("CREATE TABLE unrelated (id INT PRIMARY KEY)")

        val result = testDb.migrate("plugin_a") { sql("migrations/plugin_a") }

        assertEquals(1, result.migrationsExecuted)
        assertTrue(testDb.tableExists("a_items"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun existingTablesAreBaselinedAndIdempotentV1Passes(
        db: String,
    ) = withTestDatabase(db) { testDb ->
        testDb.execute("CREATE TABLE a_items (id INT PRIMARY KEY, name VARCHAR(64) NOT NULL)")

        val result = testDb.migrate("plugin_a") { sql("migrations/plugin_a") }

        assertEquals(1, result.migrationsExecuted)
        assertEquals("0", result.initialVersion)
        assertEquals("1", result.targetVersion)
        assertEquals(
            listOf("0:BASELINE:true", "1:SQL:true"),
            testDb.history("flyway_schema_history_plugin_a"),
        )
    }

    /**
     * Postgres runs the failing SQL migration in a transaction and rolls it back, so no row is
     * left. MariaDB DDL is not transactional, so Flyway records every failed migration there with
     * `success = false`.
     */
    @ParameterizedTest
    @MethodSource("databases")
    fun failingSqlMigrationIsRecordedOnlyWhereDdlIsNonTransactional(db: String) = withTestDatabase(
        db,
    ) { testDb ->
        val error = assertFailsWith<IllegalStateException> {
            testDb.migrate("failing") { sql("migrations/failing") }
        }

        val message = assertNotNull(error.message)
        assertContains(message, "Migration 'failing' failed at version 1 (broken)")
        assertContains(message, "V1__broken.sql")
        val expected = if (db == "mariadb") listOf("1:SQL:false") else emptyList()
        assertEquals(expected, testDb.history("flyway_schema_history_failing"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun failingJdbcMigrationIsRecordedOnlyWhereDdlIsNonTransactional(db: String) = withTestDatabase(
        db,
    ) { testDb ->
        val failing = object : JdbcMigration("1", "broken jdbc") {
            override fun migrate(connection: Connection) {
                connection.createStatement().use { it.execute("THIS IS NOT VALID SQL") }
            }
        }

        val error = assertFailsWith<IllegalStateException> {
            testDb.migrate("failing_jdbc") {
                classes(
                    failing,
                )
            }
        }

        assertContains(
            assertNotNull(error.message),
            "Migration 'failing_jdbc' failed at version 1 (broken jdbc)",
        )
        val expected = if (db == "mariadb") listOf("1:JDBC:false") else emptyList()
        assertEquals(expected, testDb.history("flyway_schema_history_failing_jdbc"))
    }

    /**
     * An [ExposedMigration] is non-transactional for Flyway, so Flyway writes a failed row on both
     * databases. On PostgreSQL that insert runs inside the transaction of Flyway's transactional
     * advisory lock and is rolled back with it, so only MariaDB keeps the row.
     */
    @ParameterizedTest
    @MethodSource("databases")
    fun failingExposedMigrationIsRecordedOnlyOnMariaDb(
        db: String,
    ) = withTestDatabase(db) { testDb ->
        val failing = object : ExposedMigration("1", "broken exposed") {
            override suspend fun R2dbcTransaction.migrate() {
                exec("THIS IS NOT VALID SQL")
            }
        }

        val error = assertFailsWith<IllegalStateException> {
            testDb.migrate("failing_exposed") {
                classes(
                    failing,
                )
            }
        }

        assertContains(
            assertNotNull(error.message),
            "Migration 'failing_exposed' failed at version 1 (broken exposed)",
        )
        val expected = if (db == "mariadb") listOf("1:JDBC:false") else emptyList()
        assertEquals(expected, testDb.history("flyway_schema_history_failing_exposed"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun missingSqlLocationFails(db: String) = withTestDatabase(db) { testDb ->
        val error = assertFailsWith<IllegalStateException> {
            testDb.migrate("missing") { sql("migrations/does_not_exist") }
        }

        assertContains(assertNotNull(error.message), "migrations/does_not_exist")
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun vendorPlaceholderRunsTheSqlFilesOfTheDatabaseVendor(
        db: String,
    ) = withTestDatabase(db) { testDb ->
        val result = testDb.migrate("vendor") { sql("migrations/vendor/{vendor}") }

        assertEquals(1, result.migrationsExecuted)
        assertEquals(listOf(testDb.database.databaseType.vendor), testDb.vendors("vendor_items"))
    }

    @Test
    fun vendorPlaceholderWithoutAFolderForTheVendorFails(
    ) = withTestDatabase("postgres") { testDb ->
        val error = assertFailsWith<IllegalStateException> {
            testDb.migrate("vendor_partial") { sql("migrations/vendor_partial/{vendor}") }
        }

        assertContains(assertNotNull(error.message), "migrations/vendor_partial/postgresql")
        assertFalse(testDb.tableExists("partial_items"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun misnamedSqlFileFails(db: String) = withTestDatabase(db) { testDb ->
        val error = assertFailsWith<IllegalStateException> {
            testDb.migrate("misnamed") { sql("migrations/misnamed") }
        }

        assertContains(assertNotNull(error.message), "V1_init.sql")
        assertFalse(testDb.tableExists("misnamed_items"))
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun customBaselineSkipsMigrationsUpToIt(db: String) = withTestDatabase(db) { testDb ->
        testDb.execute("CREATE TABLE a_items (id INT PRIMARY KEY, name VARCHAR(64) NOT NULL)")

        val result = testDb.migrate("plugin_a") {
            sql("migrations/plugin_a")
            classes(V2_JdbcInsert, V3_ExposedInsert)
            baselineVersion("2")
        }

        assertEquals(1, result.migrationsExecuted)
        assertEquals("2", result.initialVersion)
        assertEquals("3", result.targetVersion)
        assertEquals(1, testDb.count("a_items"))
        assertEquals(
            listOf("2:BASELINE:true", "3:JDBC:true"),
            testDb.history("flyway_schema_history_plugin_a"),
        )
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun laterMigrationsRunOnTopOfEarlierRuns(db: String) = withTestDatabase(db) { testDb ->
        testDb.migrate("plugin_a") { sql("migrations/plugin_a") }

        val second = testDb.migrate("plugin_a") {
            sql("migrations/plugin_a")
            classes(V2_JdbcInsert, V3_ExposedInsert)
        }

        assertEquals(2, second.migrationsExecuted)
        assertEquals("1", second.initialVersion)
        assertEquals("3", second.targetVersion)
        assertEquals(
            listOf("1:SQL:true", "2:JDBC:true", "3:JDBC:true"),
            testDb.history("flyway_schema_history_plugin_a"),
        )
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun changedChecksumOfAppliedMigrationFails(db: String) = withTestDatabase(db) { testDb ->
        class Checked(override val checksum: Int) : JdbcMigration("1", "checked") {
            override fun migrate(connection: Connection) = Unit
        }
        testDb.migrate("checked") { classes(Checked(1)) }

        val error = assertFailsWith<IllegalStateException> {
            testDb.migrate("checked") {
                classes(
                    Checked(2),
                )
            }
        }

        assertContains(assertNotNull(error.message), "checksum", ignoreCase = true)
    }

    @ParameterizedTest
    @MethodSource("databases")
    fun sqlIsFoundThroughTheConfiguredClassLoader(
        db: String,
        @TempDir dir: Path,
    ) = withTestDatabase(db) { testDb ->
        dir.resolve("isolated").createDirectories()
            .resolve("V1__isolated.sql")
            .writeText("CREATE TABLE IF NOT EXISTS isolated_items (id INT PRIMARY KEY);")

        val result = URLClassLoader(arrayOf(dir.toUri().toURL()), null).use { isolated ->
            testDb.migrate("isolated") {
                sql("isolated")
                classLoader(isolated)
            }
        }

        assertEquals(1, result.migrationsExecuted)
        assertTrue(testDb.tableExists("isolated_items"))
    }

    @Test
    fun postgresHistoryTableIsCreatedInTheApiSchema() = withTestDatabase("postgres") { testDb ->
        testDb.migrate("plugin_a") { sql("migrations/plugin_a") }

        val schema = assertIs<PGSimpleDataSource>(testDb.migrationDataSource).currentSchema
        val schemas = testDb.migrationDataSource.connection.use { c ->
            c.prepareStatement(
                "SELECT table_schema FROM information_schema.tables " +
                    "WHERE table_name = 'flyway_schema_history_plugin_a' AND table_schema IN (?, 'public')",
            ).use { s ->
                s.setString(1, schema)
                s.executeQuery().use { rs -> buildList { while (rs.next()) add(rs.getString(1)) } }
            }
        }

        assertEquals(listOf(schema), schemas)
    }
}
