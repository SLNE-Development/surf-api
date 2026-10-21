package dev.slne.surf.database.migration

import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.migration.fixtures.scan.V2_Object
import dev.slne.surf.database.migration.fixtures.scan.V3_Class
import dev.slne.surf.database.migration.internal.FlywayMigrationRunner
import dev.slne.surf.database.migration.internal.JdbcDataSources
import dev.slne.surf.database.migration.internal.MigrationClassLoader
import dev.slne.surf.database.testing.withOfflineDatabaseApi
import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.junit.jupiter.api.Test
import java.sql.Connection
import javax.sql.DataSource
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Checks the Flyway configuration built for a plan. Nothing here connects to a database.
 */
class FlywayMigrationRunnerTest {

    private val mariadb: DataSource = JdbcDataSources.create(
        ConnectionDetails(databaseType = DatabaseType.MARIADB, host = "localhost", port = 1)
    )

    private fun postgres(schema: String): DataSource = JdbcDataSources.create(
        ConnectionDetails(databaseType = DatabaseType.POSTGRESQL, schema = schema, host = "localhost", port = 1)
    )

    private fun configure(
        dataSource: DataSource = mariadb,
        name: String = "plugin_a",
        block: MigrationSpec.() -> Unit
    ): FluentConfiguration {
        val plan = MigrationSpec(javaClass.classLoader).apply(block).build(name)
        return withOfflineDatabaseApi(dataSource) { api -> FlywayMigrationRunner(api).configure(plan) }
    }

    @Test
    fun usesThePluginHistoryTable() {
        assertEquals("flyway_schema_history_myplugin", configure(name = "MyPlugin") { sql("db") }.table)
    }

    @Test
    fun usesTheMigrationDataSource() {
        assertSame(mariadb, configure { sql("db") }.dataSource)
    }

    @Test
    fun baselinesOnMigrateAtZeroByDefault() {
        val configuration = configure { sql("db") }

        assertTrue(configuration.isBaselineOnMigrate)
        assertEquals(MigrationVersion.fromVersion("0"), configuration.baselineVersion)
    }

    @Test
    fun usesTheConfiguredBaselineVersion() {
        val configuration = configure {
            sql("db")
            baselineVersion("2.1")
        }

        assertEquals(MigrationVersion.fromVersion("2.1"), configuration.baselineVersion)
    }

    @Test
    fun failsOnMissingLocationsAndMisnamedFiles() {
        val configuration = configure { sql("db") }

        assertTrue(configuration.isFailOnMissingLocations)
        assertTrue(configuration.isValidateMigrationNaming)
        assertTrue(configuration.isCleanDisabled)
    }

    @Test
    fun usesOnlyTheConfiguredSqlLocations() {
        val configuration = configure { sql("db/one", "classpath:db/two") }

        assertEquals(listOf("classpath:db/one", "classpath:db/two"), configuration.locations.map { it.descriptor })
    }

    @Test
    fun wrapsThePlanClassLoader() {
        val pluginLoader = object : ClassLoader(javaClass.classLoader) {}

        val configuration = configure {
            sql("db")
            classLoader(pluginLoader)
        }

        assertIs<MigrationClassLoader>(configuration.classLoader)
        assertSame(V2_Object::class.java, configuration.classLoader.loadClass(V2_Object::class.java.name))
    }

    @Test
    fun registersCodeMigrationsAndScannedClasses() {
        val configuration = configure {
            classes(V2_Object)
            classPackage("dev.slne.surf.database.migration.fixtures.scan")
        }

        assertEquals(
            listOf("2", "3", "5"),
            configuration.javaMigrations.map { it.version.version }.sorted()
        )
    }

    @Test
    fun deduplicatesMigrationsOfTheSameClass() {
        val configuration = configure { classes(V3_Class(), V3_Class()) }

        assertEquals(1, configuration.javaMigrations.size)
    }

    @Test
    fun keepsDistinctClassesWithTheSameVersion() {
        val first = object : JdbcMigration("7", "first") {
            override fun migrate(connection: Connection) = Unit
        }
        val second = object : JdbcMigration("7", "second") {
            override fun migrate(connection: Connection) = Unit
        }

        val configuration = configure { classes(first, second) }

        assertEquals(listOf("first", "second"), configuration.javaMigrations.map { it.description })
    }

    @Test
    fun postgresHistoryTableUsesTheDataSourceSchema() {
        assertEquals("plugin_schema", configure(postgres("plugin_schema")) { sql("db") }.defaultSchema)
    }

    @Test
    fun postgresWithBlankSchemaKeepsTheDefault() {
        assertNull(configure(postgres(" ")) { sql("db") }.defaultSchema)
    }

    @Test
    fun mariaDbKeepsTheDefaultSchema() {
        assertNull(configure(mariadb) { sql("db") }.defaultSchema)
    }

    @Test
    fun failsWithoutMigrationDataSource() {
        val plan = MigrationSpec(javaClass.classLoader).apply { sql("db") }.build("plugin_a")

        val error = withOfflineDatabaseApi { api ->
            assertFailsWith<IllegalStateException> { FlywayMigrationRunner(api).configure(plan) }
        }

        assertContains(assertNotNull(error.message), "migrationDataSource")
    }

    @Test
    fun failsWhenAScannedPackageCannotBeInstantiated() {
        assertFailsWith<IllegalStateException> {
            configure { classPackage("dev.slne.surf.database.migration.fixtures.badscan") }
        }
    }
}
