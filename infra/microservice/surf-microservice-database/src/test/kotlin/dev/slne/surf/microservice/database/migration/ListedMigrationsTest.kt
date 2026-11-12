package dev.slne.surf.microservice.database.migration

import org.junit.jupiter.api.io.TempDir
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ListedMigrationsTest {

    @TempDir
    lateinit var root: Path

    @Test
    fun `returns nothing without the resource`() {
        assertTrue(loadListedMigrations(TestClassPath(root).classLoader()).isEmpty())
    }

    @Test
    fun `creates a class through its no-argument constructor`() {
        val migrations = load(TestMigrations.ClassMigration::class.java.name)

        assertIs<TestMigrations.ClassMigration>(migrations.single())
    }

    @Test
    fun `uses the instance of an object`() {
        val migrations = load(TestMigrations.ObjectMigration::class.java.name)

        assertSame(TestMigrations.ObjectMigration, migrations.single())
    }

    @Test
    fun `ignores blank lines and surrounding whitespace`() {
        val migrations = load("", "  ${TestMigrations.ClassMigration::class.java.name}  ", "\r", "")

        assertIs<TestMigrations.ClassMigration>(migrations.single())
    }

    @Test
    fun `reads every copy of the resource and loads each class once`(@TempDir otherRoot: Path) {
        val className = TestMigrations.ClassMigration::class.java.name
        TestClassPath(root).withListedMigrations(className)
        TestClassPath(otherRoot).withListedMigrations(className, TestMigrations.ObjectMigration::class.java.name)
        val roots = arrayOf(root.toUri().toURL(), otherRoot.toUri().toURL())

        val migrations = loadListedMigrations(URLClassLoader(roots, javaClass.classLoader))

        assertEquals(
            listOf<Class<*>>(TestMigrations.ClassMigration::class.java, TestMigrations.ObjectMigration::class.java),
            migrations.map { it.javaClass },
        )
    }

    @Test
    fun `rejects a class that is not a migration`() {
        val failure = assertFailsWith<IllegalStateException> { load(TestMigrations.NotAMigration::class.java.name) }

        assertContains(failure.message.orEmpty(), "does not extend")
    }

    @Test
    fun `rejects a missing class`() {
        val failure = assertFailsWith<IllegalStateException> { load("dev.slne.surf.Missing") }

        assertContains(failure.message.orEmpty(), "dev.slne.surf.Missing")
    }

    @Test
    fun `rejects a class without a no-argument constructor`() {
        val failure = assertFailsWith<IllegalStateException> {
            load(TestMigrations.ConstructorArgumentMigration::class.java.name)
        }

        assertContains(failure.message.orEmpty(), "public no-argument constructor")
    }

    @Test
    fun `reports a failing constructor with its cause`() {
        val failure = assertFailsWith<IllegalStateException> {
            load(TestMigrations.FailingMigration::class.java.name)
        }

        assertEquals("Could not instantiate migration ${TestMigrations.FailingMigration::class.java.name}", failure.message)
        assertEquals("constructor failed", failure.cause?.cause?.message)
    }

    private fun load(vararg lines: String) =
        loadListedMigrations(TestClassPath(root).withListedMigrations(*lines).classLoader())
}
