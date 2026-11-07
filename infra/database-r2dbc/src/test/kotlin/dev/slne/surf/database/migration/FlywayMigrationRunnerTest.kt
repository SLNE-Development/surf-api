package dev.slne.surf.database.migration

import dev.slne.surf.api.core.config.type.number.IntOr
import dev.slne.surf.database.config.ConnectionDetails
import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.database.migration.fixtures.mixedscan.V2_Default
import dev.slne.surf.database.migration.fixtures.scan.V2_Object
import dev.slne.surf.database.migration.internal.FlywayMigrationRunner
import dev.slne.surf.database.migration.internal.JdbcDataSources
import dev.slne.surf.database.migration.internal.MigrationClassLoader
import dev.slne.surf.database.testing.withOfflineDatabaseApi
import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.jetbrains.exposed.v1.core.vendors.DatabaseDialect
import org.jetbrains.exposed.v1.core.vendors.H2Dialect
import org.jetbrains.exposed.v1.core.vendors.MariaDBDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
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

    private class Versioned(version: String) : JdbcMigration(version, "versioned $version") {
        override fun migrate(connection: Connection) = Unit
    }

    private val mariadb: DataSource = JdbcDataSources.create(
        ConnectionDetails(databaseType = DatabaseType.MARIADB, host = "localhost", port = IntOr.Default(1))
    )

    private fun postgres(schema: String): DataSource = JdbcDataSources.create(
        ConnectionDetails(databaseType = DatabaseType.POSTGRESQL, schema = schema, host = "localhost", port = IntOr.Default(1))
    )

    private fun configure(
        dataSource: DataSource = mariadb,
        name: String = "plugin_a",
        dialect: DatabaseDialect = MariaDBDialect(),
        block: MigrationSpec.() -> Unit
    ): FluentConfiguration {
        val plan = MigrationSpec(javaClass.classLoader).apply(block).build(name)
        return withOfflineDatabaseApi(dataSource, dialect) { api -> FlywayMigrationRunner(api).configure(plan) }
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
    fun usesStrictFlywaySettings() {
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
    fun replacesTheVendorPlaceholderForMariaDb() {
        val configuration = configure { sql("db/{vendor}", "db/{vendor}_extra") }

        assertEquals(
            listOf("classpath:db/mariadb", "classpath:db/mariadb_extra"),
            configuration.locations.map { it.descriptor }
        )
    }

    @Test
    fun replacesTheVendorPlaceholderForPostgres() {
        val configuration = configure(postgres("public"), dialect = PostgreSQLDialect()) { sql("db/{vendor}") }

        assertEquals(listOf("classpath:db/postgresql"), configuration.locations.map { it.descriptor })
    }

    @Test
    fun dialectWithoutDatabaseTypeConfiguresLocationsWithoutPlaceholder() {
        val configuration = configure(dialect = H2Dialect()) { sql("db/common") }

        assertEquals(listOf("classpath:db/common"), configuration.locations.map { it.descriptor })
    }

    @Test
    fun dialectWithoutDatabaseTypeFailsOnTheVendorPlaceholder() {
        val failure = assertFailsWith<IllegalStateException> {
            configure(name = "plugin_h2", dialect = H2Dialect()) { sql("db/common", "db/{vendor}") }
        }

        val message = failure.message.orEmpty()
        assertContains(message, "plugin_h2")
        assertContains(message, "db/{vendor}")
        assertContains(message, "H2")
    }

    @Test
    fun resolvedVendorLocationIsReadFromThePluginLoaderOnly() {
        val emptyPluginLoader = object : ClassLoader(null) {}

        val configuration = configure {
            sql("migrations/vendor/{vendor}")
            classLoader(emptyPluginLoader)
        }

        assertNotNull(javaClass.classLoader.getResource(MARIADB_VENDOR_SQL))
        assertNull(configuration.classLoader.getResource(MARIADB_VENDOR_SQL))
    }

    @Test
    fun withoutSqlNoDefaultLocationIsUsed() {
        assertTrue(configure { classes(V2_Object) }.locations.isEmpty())
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
    fun classFoundByNestedPackagesIsRegisteredOnce() {
        val configuration = configure {
            classPackage("dev.slne.surf.database.migration.fixtures.scan")
            classPackage("dev.slne.surf.database.migration.fixtures.scan.nested")
        }

        assertEquals(listOf("2", "3", "5"), configuration.javaMigrations.map { it.version.version }.sorted())
    }

    @Test
    fun keepsEveryVersionOfTheSameClass() {
        val configuration = configure { classes(Versioned("2"), Versioned("3")) }

        assertEquals(listOf("2", "3"), configuration.javaMigrations.map { it.version.version })
    }

    @Test
    fun listedInstanceWithAnotherVersionKeepsTheScannedOne() {
        val configuration = configure {
            classes(V2_Default("3"))
            classPackage("dev.slne.surf.database.migration.fixtures.mixedscan")
        }

        assertEquals(listOf("2", "3"), configuration.javaMigrations.map { it.version.version }.sorted())
    }

    @Test
    fun listedInstanceWithTheScannedVersionReplacesTheScannedOne() {
        val configuration = configure {
            classes(V2_Default())
            classPackage("dev.slne.surf.database.migration.fixtures.mixedscan")
        }

        assertEquals(listOf("2"), configuration.javaMigrations.map { it.version.version })
    }

    @Test
    fun failsWhenAScannedPackageHasNoMigrations() {
        val error = assertFailsWith<IllegalStateException> {
            configure { classPackage("dev.slne.surf.database.migration.fixtures.doesnotexist") }
        }

        assertContains(assertNotNull(error.message), "fixtures.doesnotexist")
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

    private companion object {
        const val MARIADB_VENDOR_SQL = "migrations/vendor/mariadb/V1__vendor_items.sql"
    }
}
