package dev.slne.surf.database.migration.fixtures.nestedscan

import dev.slne.surf.database.migration.JdbcMigration
import dev.slne.surf.database.migration.SurfMigration
import java.sql.Connection

/**
 * Named nested migrations are found; the anonymous one below is not.
 */
object Grouped {
    object V1_NestedObject : JdbcMigration("1", "nested object") {
        override fun migrate(connection: Connection) = Unit
    }

    class V2_NestedClass : JdbcMigration("2", "nested class") {
        override fun migrate(connection: Connection) = Unit
    }
}

val anonymous: SurfMigration = object : JdbcMigration("99", "anonymous") {
    override fun migrate(connection: Connection) = Unit
}
