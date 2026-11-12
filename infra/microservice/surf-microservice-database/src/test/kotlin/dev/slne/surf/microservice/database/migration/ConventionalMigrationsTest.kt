package dev.slne.surf.microservice.database.migration

import dev.slne.surf.database.config.DatabaseType
import dev.slne.surf.microservice.database.DatabaseCustomizer
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConventionalMigrationsTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `skips migrations when nothing is found and no customizer exists`() {
        assertNull(select(customizer = null, TestClassPath(root).classLoader()))
    }

    @Test
    fun `finds the SQL location when the class loader has it`() {
        val classLoader = TestClassPath(root).withSqlMigrationDirectory().classLoader()

        val migrations = assertNotNull(select(customizer = null, classLoader))

        assertEquals(ConventionalMigrations.SQL_LOCATION, migrations.sqlLocation)
        assertTrue(migrations.classes.isEmpty())
    }

    @Test
    fun `uses the vendor location when the class loader has a folder for the database vendor`() {
        val classLoader = TestClassPath(root)
            .withSqlMigrationDirectory()
            .withVendorSqlMigrationDirectory(DatabaseType.MARIADB)
            .withVendorSqlMigrationDirectory(DatabaseType.POSTGRESQL)
            .classLoader()

        val migrations = assertNotNull(select(customizer = null, classLoader, DatabaseType.POSTGRESQL))

        assertEquals("db/migration/{vendor}", migrations.sqlLocation)
    }

    @Test
    fun `uses the vendor location when only the database vendor has a folder`() {
        val classLoader = TestClassPath(root).withVendorSqlMigrationDirectory(DatabaseType.MARIADB).classLoader()

        val migrations = assertNotNull(select(customizer = null, classLoader, DatabaseType.MARIADB))

        assertEquals(ConventionalMigrations.VENDOR_SQL_LOCATION, migrations.sqlLocation)
    }

    @Test
    fun `fails when vendor folders exist but none for the database vendor`() {
        val classLoader = TestClassPath(root).withVendorSqlMigrationDirectory(DatabaseType.MARIADB).classLoader()

        val failure = assertFailsWith<IllegalStateException> {
            select(customizer = null, classLoader, DatabaseType.POSTGRESQL)
        }

        assertEquals(
            "SQL migrations exist for db/migration/mariadb, but not for the configured postgresql database; " +
                    "add db/migration/postgresql",
            failure.message,
        )
    }

    @Test
    fun `does not check vendor folders when the customizer disables migrations`() {
        val classLoader = TestClassPath(root).withVendorSqlMigrationDirectory(DatabaseType.MARIADB).classLoader()

        assertNull(select(Customizer(runMigrations = false), classLoader, DatabaseType.POSTGRESQL))
    }

    @Test
    fun `finds listed migration classes`() {
        val classLoader = TestClassPath(root)
            .withListedMigrations(TestMigrations.ObjectMigration::class.java.name)
            .classLoader()

        val migrations = assertNotNull(select(customizer = null, classLoader))

        assertNull(migrations.sqlLocation)
        assertEquals(listOf(TestMigrations.ObjectMigration), migrations.classes)
    }

    @Test
    fun `runs migrations for a customizer even if no convention found anything`() {
        val classLoader = TestClassPath(root).classLoader()

        val migrations = select(Customizer(runMigrations = true), classLoader)

        assertTrue(assertNotNull(migrations).isEmpty)
    }

    @Test
    fun `runs conventional migrations for a customizer`() {
        val classLoader = TestClassPath(root).withSqlMigrationDirectory().classLoader()

        val migrations = select(Customizer(runMigrations = true), classLoader)

        assertEquals(ConventionalMigrations.SQL_LOCATION, assertNotNull(migrations).sqlLocation)
    }

    @Test
    fun `skips migrations when the customizer disables them`() {
        val classLoader = TestClassPath(root).withSqlMigrationDirectory().classLoader()

        assertNull(select(Customizer(runMigrations = false), classLoader))
    }

    @Test
    fun `loads no classes when the customizer disables migrations`() {
        val classLoader = TestClassPath(root).withListedMigrations("dev.slne.surf.Missing").classLoader()

        assertNull(select(Customizer(runMigrations = false), classLoader))
    }

    private fun select(
        customizer: DatabaseCustomizer?,
        classLoader: ClassLoader,
        databaseType: DatabaseType = DatabaseType.MARIADB,
    ) = ConventionalMigrations.select(customizer, classLoader, databaseType)

    private class Customizer(override val runMigrations: Boolean) : DatabaseCustomizer
}
