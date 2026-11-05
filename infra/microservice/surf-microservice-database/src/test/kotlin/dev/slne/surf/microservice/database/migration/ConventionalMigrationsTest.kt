package dev.slne.surf.microservice.database.migration

import dev.slne.surf.microservice.database.DatabaseCustomizer
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConventionalMigrationsTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `skips migrations when nothing is found and no customizer exists`() {
        assertNull(ConventionalMigrations.select(customizer = null, TestClassPath(root).classLoader()))
    }

    @Test
    fun `finds the SQL location when the class loader has it`() {
        val classLoader = TestClassPath(root).withSqlMigrationDirectory().classLoader()

        val migrations = assertNotNull(ConventionalMigrations.select(customizer = null, classLoader))

        assertEquals(ConventionalMigrations.SQL_LOCATION, migrations.sqlLocation)
        assertTrue(migrations.classes.isEmpty())
    }

    @Test
    fun `finds listed migration classes`() {
        val classLoader = TestClassPath(root)
            .withListedMigrations(TestMigrations.ObjectMigration::class.java.name)
            .classLoader()

        val migrations = assertNotNull(ConventionalMigrations.select(customizer = null, classLoader))

        assertNull(migrations.sqlLocation)
        assertEquals(listOf(TestMigrations.ObjectMigration), migrations.classes)
    }

    @Test
    fun `runs migrations for a customizer even if no convention found anything`() {
        val classLoader = TestClassPath(root).classLoader()

        val migrations = ConventionalMigrations.select(Customizer(runMigrations = true), classLoader)

        assertTrue(assertNotNull(migrations).isEmpty)
    }

    @Test
    fun `runs conventional migrations for a customizer`() {
        val classLoader = TestClassPath(root).withSqlMigrationDirectory().classLoader()

        val migrations = ConventionalMigrations.select(Customizer(runMigrations = true), classLoader)

        assertEquals(ConventionalMigrations.SQL_LOCATION, assertNotNull(migrations).sqlLocation)
    }

    @Test
    fun `skips migrations when the customizer disables them`() {
        val classLoader = TestClassPath(root).withSqlMigrationDirectory().classLoader()

        assertNull(ConventionalMigrations.select(Customizer(runMigrations = false), classLoader))
    }

    @Test
    fun `loads no classes when the customizer disables migrations`() {
        val classLoader = TestClassPath(root).withListedMigrations("dev.slne.surf.Missing").classLoader()

        assertNull(ConventionalMigrations.select(Customizer(runMigrations = false), classLoader))
    }

    private class Customizer(override val runMigrations: Boolean) : DatabaseCustomizer
}
