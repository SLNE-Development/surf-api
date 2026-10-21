package dev.slne.surf.database.migration

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.Connection
import kotlin.test.assertEquals

class MigrationSpecTest {

    private class V1 : JdbcMigration("1", "one") {
        override fun migrate(connection: Connection) = Unit
    }

    private fun spec(block: MigrationSpec.() -> Unit = { sql("db/migrations") }) =
        MigrationSpec(javaClass.classLoader).apply(block)

    @Test
    fun historyTableIsDerivedFromName() {
        assertEquals("flyway_schema_history_myplugin", spec().build("MyPlugin").historyTable)
    }

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
}
