package dev.slne.surf.microservice.database.migration

import dev.slne.surf.database.config.DatabaseType
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConventionalMigrationsTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `finds nothing when the application ships no migrations`() {
        assertNull(discover(TestClassPath(root).classLoader()))
    }

    @Test
    fun `finds the SQL location when the class loader has it`() {
        val classLoader = TestClassPath(root).withSqlMigrationDirectory().classLoader()

        val migrations = assertNotNull(discover(classLoader))

        assertEquals(ConventionalMigrations.SQL_LOCATION, migrations.sqlLocation)
        assertEquals(emptyList(), migrations.classNames)
    }

    @Test
    fun `uses the vendor location when the class loader has a folder for the database vendor`() {
        val classLoader = TestClassPath(root)
            .withSqlMigrationDirectory()
            .withVendorSqlMigrationDirectory(DatabaseType.MARIADB)
            .withVendorSqlMigrationDirectory(DatabaseType.POSTGRESQL)
            .classLoader()

        val migrations = assertNotNull(discover(classLoader, DatabaseType.POSTGRESQL))

        assertEquals("db/migration/{vendor}", migrations.sqlLocation)
    }

    @Test
    fun `uses the vendor location when only the database vendor has a folder`() {
        val classLoader = TestClassPath(root).withVendorSqlMigrationDirectory(DatabaseType.MARIADB).classLoader()

        val migrations = assertNotNull(discover(classLoader, DatabaseType.MARIADB))

        assertEquals(ConventionalMigrations.VENDOR_SQL_LOCATION, migrations.sqlLocation)
    }

    @Test
    fun `fails when vendor folders exist but none for the database vendor`() {
        val classLoader = TestClassPath(root).withVendorSqlMigrationDirectory(DatabaseType.MARIADB).classLoader()

        val failure = assertFailsWith<IllegalStateException> { discover(classLoader, DatabaseType.POSTGRESQL) }

        assertEquals(
            "SQL migrations exist for db/migration/mariadb, but not for the configured postgresql database; " +
                    "add db/migration/postgresql",
            failure.message,
        )
    }

    @Test
    fun `finds the migration classes by name, not their package`() {
        val classLoader = TestClassPath(root).classLoader()

        val migrations = assertNotNull(discover(classLoader, packages = listOf(MIGRATION_PACKAGE, NATS_PACKAGE)))

        assertNull(migrations.sqlLocation)
        assertEquals(DISCOVERED, migrations.classNames)
    }

    @Test
    fun `skips anonymous and local migration classes`() {
        val migrations = assertNotNull(discover(TestClassPath(root).classLoader(), packages = listOf(MIGRATION_PACKAGE)))

        assertEquals(DISCOVERED, migrations.classNames)
    }

    @Test
    fun `skips open classes whose constructor takes parameters and keeps those without`() {
        val migrations = assertNotNull(discover(TestClassPath(root).classLoader(), packages = listOf(MIGRATION_PACKAGE)))

        assertFalse(OPEN_MIGRATION in migrations.classNames)
        assertContains(migrations.classNames, OPEN_DEFAULT_MIGRATION)
    }

    @Test
    fun `finds a companion object migration by the name of the companion class`() {
        val migrations = assertNotNull(discover(TestClassPath(root).classLoader(), packages = listOf(MIGRATION_PACKAGE)))

        assertContains(migrations.classNames, COMPANION_MIGRATION)
    }

    @Test
    fun `finds nothing in a package without migration classes`() {
        assertNull(discover(TestClassPath(root).classLoader(), packages = listOf(NATS_PACKAGE)))
    }

    @Test
    fun `lists a class once when packages overlap`() {
        val classLoader = TestClassPath(root).classLoader()

        val migrations = assertNotNull(discover(classLoader, packages = listOf(MIGRATION_PACKAGE, "dev.slne.surf.microservice.database")))

        assertEquals(DISCOVERED, migrations.classNames)
    }

    @Test
    fun `combines SQL and migration classes`() {
        val classLoader = TestClassPath(root).withSqlMigrationDirectory().classLoader()

        val migrations = assertNotNull(discover(classLoader, packages = listOf(MIGRATION_PACKAGE)))

        assertEquals(
            ConventionalMigrations(ConventionalMigrations.SQL_LOCATION, DISCOVERED),
            migrations,
        )
    }

    private fun discover(
        classLoader: ClassLoader,
        databaseType: DatabaseType = DatabaseType.MARIADB,
        packages: List<String> = emptyList(),
    ) = ConventionalMigrations.discover(classLoader, databaseType, packages)

    private companion object {
        const val MIGRATION_PACKAGE = "dev.slne.surf.microservice.database.migration"
        const val NATS_PACKAGE = "dev.slne.surf.microservice.database.nats"
        const val CLASS_MIGRATION = "$MIGRATION_PACKAGE.TestMigrations\$ClassMigration"
        const val OBJECT_MIGRATION = "$MIGRATION_PACKAGE.TestMigrations\$ObjectMigration"
        const val COMPANION_MIGRATION = "$MIGRATION_PACKAGE.TestMigrations\$WithCompanion\$Companion"
        const val OPEN_MIGRATION = "$MIGRATION_PACKAGE.TestMigrations\$OpenBase"
        const val OPEN_DEFAULT_MIGRATION = "$MIGRATION_PACKAGE.TestMigrations\$OpenWithDefaultConstructor"
        val DISCOVERED = listOf(CLASS_MIGRATION, OBJECT_MIGRATION, OPEN_DEFAULT_MIGRATION, COMPANION_MIGRATION)
    }
}
