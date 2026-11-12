package dev.slne.surf.database.migration

import dev.slne.surf.database.migration.fixtures.plugina.V2_JdbcInsert
import dev.slne.surf.database.migration.fixtures.plugina.V3_ExposedInsert
import dev.slne.surf.database.migration.internal.FlywayJavaMigrationAdapter
import dev.slne.surf.database.testing.withOfflineDatabaseApi
import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.api.configuration.Configuration
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.flywaydb.core.api.migration.Context
import org.junit.jupiter.api.Test
import java.lang.reflect.Proxy
import java.sql.Connection
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FlywayJavaMigrationAdapterTest {

    private class Checksummed : JdbcMigration("2.1", "with checksum") {
        override val checksum: Int = 42
        override fun migrate(connection: Connection) = Unit
    }

    private class Recording : JdbcMigration("1", "recording") {
        var connection: Connection? = null
        override fun migrate(connection: Connection) {
            this.connection = connection
        }
    }

    private fun adapt(migration: SurfMigration, block: (FlywayJavaMigrationAdapter) -> Unit) =
        withOfflineDatabaseApi { api -> block(FlywayJavaMigrationAdapter(api, migration)) }

    @Test
    fun exposesVersionAndDescription() = adapt(Checksummed()) { adapter ->
        assertEquals(MigrationVersion.fromVersion("2.1"), adapter.version)
        assertEquals("with checksum", adapter.description)
    }

    @Test
    fun exposesChecksum() = adapt(Checksummed()) { adapter ->
        assertEquals(42, adapter.checksum)
    }

    @Test
    fun checksumDefaultsToNull() = adapt(V2_JdbcInsert) { adapter ->
        assertNull(adapter.checksum)
    }

    @Test
    fun jdbcMigrationRunsInFlywaysTransaction() = adapt(V2_JdbcInsert) { adapter ->
        assertTrue(adapter.canExecuteInTransaction())
    }

    @Test
    fun exposedMigrationRunsOutsideFlywaysTransaction() = adapt(V3_ExposedInsert) { adapter ->
        assertFalse(adapter.canExecuteInTransaction())
    }

    @Test
    fun resolvedMigrationNamesTheWrappedClassAsScript() = adapt(Checksummed()) { adapter ->
        val resolved = adapter.getResolvedMigration(FluentConfiguration(), null)

        assertEquals(Checksummed::class.java.name, resolved.script)
        assertEquals(MigrationVersion.fromVersion("2.1"), resolved.version)
        assertEquals("with checksum", resolved.description)
        assertEquals(42, resolved.checksum)
        assertEquals(adapter.type, resolved.type)
    }

    @Test
    fun jdbcMigrationReceivesTheContextConnection() {
        val migration = Recording()
        val connection = Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(Connection::class.java)
        ) { _, _, _ -> null } as Connection
        val context = object : Context {
            override fun getConfiguration(): Configuration = FluentConfiguration()
            override fun getConnection(): Connection = connection
        }

        adapt(migration) { it.migrate(context) }

        assertSame(connection, migration.connection)
    }
}
