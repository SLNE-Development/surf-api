package dev.slne.surf.database.migration.fixtures.scan

import dev.slne.surf.database.migration.JdbcMigration
import java.sql.Connection

abstract class Base : JdbcMigration("9", "base") {
    override fun migrate(connection: Connection) = Unit
}
