package dev.slne.surf.database.migration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import kotlin.test.assertEquals
import kotlin.test.assertSame

class MigrationSpecTest {

    private class V1 : JdbcMigration("1", "one") {
        override fun migrate(connection: Connection) = Unit
    }

    private fun spec(block: MigrationSpec.() -> Unit = { sql("db/migrations") }) =
        MigrationSpec(javaClass.classLoader).apply(block)

    @Test
    fun rejectsInvalidNames() {
        listOf("my-plugin", "", "a".repeat(41)).forEach {
            assertThrows<IllegalArgumentException> { spec().build(it) }
        }
    }

    @Test
    fun sqlLocationsGetClasspathPrefix() {
        val plan = spec {
            sql("db/migrations")
            sql("classpath:db/other")
        }.build("p")
        assertEquals(listOf("classpath:db/migrations", "classpath:db/other"), plan.sqlLocations)
    }

    @Test
    fun defaultsBaselineToZero() {
        assertEquals("0", spec().build("p").baselineVersion)
    }

    @Test
    fun emptySpecIsRejected() {
        assertThrows<IllegalArgumentException> { spec { }.build("p") }
    }

    @Test
    fun duplicateMigrationInstancesAreDeduplicated() {
        val v1 = V1()
        assertEquals(1, spec { classes(v1, v1) }.build("p").migrations.size)
    }

    @Test
    fun acceptsValidBaselineVersions() {
        listOf("0", "1", "2.1", "10.0.3").forEach {
            assertEquals(it, spec { sql("db/migrations"); baselineVersion(it) }.build("p").baselineVersion)
        }
    }

    @Test
    fun acceptsValidNames() {
        listOf("p", "plugin_2", "a".repeat(40), "My_Plugin").forEach {
            assertEquals("flyway_schema_history_${it.lowercase()}", spec().build(it).historyTable)
        }
    }

    @Test
    fun rejectsBlankSqlLocationOrPackage() {
        listOf<MigrationSpec.(String) -> Unit>({ sql(it) }, { classPackage(it) }).forEach { add ->
            listOf("", " ").forEach {
                assertThrows<IllegalArgumentException>("'$it' must be rejected") { spec().add(it) }
            }
        }
    }

    @Test
    fun eachEntryAloneIsEnough() {
        assertEquals(listOf("one"), spec { classes(V1()) }.build("p").migrations.map { it.description })
        assertEquals(listOf("a.b"), spec { classPackage("a.b") }.build("p").packages)
    }

    @Test
    fun duplicateLocationsAndPackagesAreDeduplicated() {
        val plan = spec {
            sql("db/migrations", "classpath:db/migrations")
            classPackage("a.b")
            classPackage("a.b")
        }.build("p")

        assertEquals(listOf("classpath:db/migrations"), plan.sqlLocations)
        assertEquals(listOf("a.b"), plan.packages)
    }

    @Test
    fun keepsMigrationOrder() {
        val first = V1()
        val second = V1()
        assertEquals(listOf<SurfMigration>(first, second), spec { classes(first); classes(second) }.build("p").migrations)
    }

    @Test
    fun classLoaderDefaultsToTheGivenLoader() {
        val loader = object : ClassLoader(null) {}
        assertSame(loader, MigrationSpec(loader).apply { sql("db") }.build("p").classLoader)
    }

    @Test
    fun classLoaderCanBeOverridden() {
        val loader = object : ClassLoader(null) {}
        assertSame(loader, spec { sql("db"); classLoader(loader) }.build("p").classLoader)
    }

    @Test
    fun lastBaselineVersionWins() {
        assertEquals("3", spec { sql("db"); baselineVersion("2"); baselineVersion("3") }.build("p").baselineVersion)
    }

    @Test
    fun rejectedBaselineVersionKeepsThePreviousOne() {
        val spec = spec { sql("db"); baselineVersion("2") }
        assertThrows<IllegalArgumentException> { spec.baselineVersion("x") }
        assertEquals("2", spec.build("p").baselineVersion)
    }

    @Test
    fun rejectsInvalidBaselineVersions() {
        listOf("", " ", "v1", "1.", ".1", "1..2", "1_2", "abc", "1 2").forEach {
            assertThrows<IllegalArgumentException>("'$it' must be rejected") { spec().baselineVersion(it) }
        }
    }
}
