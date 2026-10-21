package dev.slne.surf.database.migration.fixtures.scan

import dev.slne.surf.database.migration.JdbcMigration
import java.sql.Connection

object V2_Object : JdbcMigration("2", "object") {
    override fun migrate(connection: Connection) {
        connection.createStatement().use { it.execute("SELECT 1") }
    }
}
